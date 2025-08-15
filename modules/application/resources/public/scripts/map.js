import Map from "https://esm.sh/ol/Map.js";
import View from "https://esm.sh/ol/View.js";
import TileLayer from "https://esm.sh/ol/layer/Tile.js";
import VectorLayer from "https://esm.sh/ol/layer/Vector.js";
import OSM from "https://esm.sh/ol/source/OSM.js";
import VectorSource from "https://esm.sh/ol/source/Vector.js";
import GeoJSON from "https://esm.sh/ol/format/GeoJSON.js";
import {Fill, Stroke, Style, Circle as CircleStyle} from "https://esm.sh/ol/style.js";

const source = new VectorSource();
const layer  = new VectorLayer({
  source,
  style: new Style({
    stroke: new Stroke({ width: 2 }),
    fill: new Fill({ opacity: 0.15 }),
    image: new CircleStyle({ radius: 5, fill: new Fill({}) })
  })
});

const map = new Map({
  target: "map",
  layers: [ new TileLayer({ source: new OSM() }), layer ],
  view: new View({ center: [0,0], zoom: 2 })
});

const gj = new GeoJSON({ dataProjection: "EPSG:4326", featureProjection: "EPSG:3857" });

async function streamNdjson(url) {
  const resp = await fetch(url, {
    headers: { "Accept": "application/x-ndjson, application/json" },
    cache: "no-store"
  });
  if (!resp.ok || !resp.body) throw new Error(`HTTP ${resp.status}`);

  const reader = resp.body.pipeThrough(new TextDecoderStream()).getReader();
  let buf = "", firstAdd = true;

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buf += value;

    const lines = buf.split(/\r?\n/);
    buf = lines.pop() ?? "";

    for (const line of lines) {
      const t = line.trim();
      if (!t) continue;
      try {
        const json = JSON.parse(t);
        const feats = gj.readFeatures(json);
        if (feats?.length) {
          source.addFeatures(feats);
          if (firstAdd) {
            firstAdd = false;
            const extent = source.getExtent();
            if (isFinite(extent[0])) map.getView().fit(extent, { padding: [20,20,20,20], duration: 250 });
          }
        }
      } catch (e) {
        console.warn("Bad JSON line", e);
      }
    }
  }

  const last = buf.trim();
  if (last) {
    try { source.addFeatures(gj.readFeatures(JSON.parse(last))); } catch {}
  }
}

streamNdjson("/jobs/viewer/3c654319-0017-4355-b20a-1f0b5a555ddd/geojsonseq").catch(console.error);
