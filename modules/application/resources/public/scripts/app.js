import Map from "https://esm.sh/ol/Map.js"
import View from "https://esm.sh/ol/View.js"
import TileLayer from "https://esm.sh/ol/layer/Tile.js"
import VectorLayer from "https://esm.sh/ol/layer/Vector.js"
import OSM from "https://esm.sh/ol/source/OSM.js"
import VectorSource from "https://esm.sh/ol/source/Vector.js"
import GeoJSON from "https://esm.sh/ol/format/GeoJSON.js"
import { Fill, Stroke, Style, Circle as CircleStyle } from "https://esm.sh/ol/style.js"

/**
 * Mount a streaming GeoJSON Text Sequence (one GeoJSON object per line) OpenLayers map.
 * @param {string} targetElementId - The DOM element id to mount the map into.
 * @param {string} geoJsonSeqUrl - The GeoJSON Text Sequence endpoint URL.
 * @returns {{map: Map, source: VectorSource, stop: () => void}}
 */
export function mountGeoJsonSeqMap(targetElementId, geoJsonSeqUrl) {
  const vectorSource = new VectorSource()

  const vectorLayer = new VectorLayer({
    source: vectorSource,
    style: new Style({
      stroke: new Stroke({ width: 1 }),
      fill: new Fill({ color: "#ffd3acdf" }),
      image: new CircleStyle({ radius: 5, fill: new Fill({ color: "red" }) })
    })
  })

  const mapInstance = new Map({
    target: targetElementId,
    layers: [new TileLayer({ source: new OSM() }), vectorLayer],
    view: new View({ center: [0, 0], zoom: 2 })
  })

  const geoJsonFormat = new GeoJSON({
    dataProjection: "EPSG:4326",
    featureProjection: "EPSG:3857"
  })

  const abortController = new AbortController()
  const { signal } = abortController;

  (async () => {
    const response = await fetch(geoJsonSeqUrl, {
      headers: { "Accept": "application/geo+json-seq" },
      cache: "no-store",
      signal
    })

    if (!response.ok || !response.body) {
      throw new Error(`HTTP ${response.status}`)
    }

    const textDecoderStreamReader = response.body
      .pipeThrough(new TextDecoderStream())
      .getReader()

    let chunkBuffer = ""
    let isFirstFeatureBatch = true

    while (true) {
      const { value: chunkText, done } = await textDecoderStreamReader.read()
      if (done) break

      chunkBuffer += chunkText

      const lines = chunkBuffer.split(/\r?\n/)
      chunkBuffer = lines.pop() ?? "" // keep the last (possibly partial) line

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        try {
          const features = geoJsonFormat.readFeatures(JSON.parse(trimmed))
          if (features && features.length > 0) {
            vectorSource.addFeatures(features)

            if (isFirstFeatureBatch) {
              isFirstFeatureBatch = false
              const extent = vectorSource.getExtent()
              if (isFinite(extent[0])) {
                mapInstance.getView().fit(extent, {
                  padding: [20, 20, 20, 20],
                  duration: 250
                })
              }
            }
          }
        } catch (error) {
          console.warn(`[${targetElementId}] Invalid JSON line`, error)
        }
      }
    }

    // Flush any final complete line
    const finalLine = chunkBuffer.trim()

    if (finalLine) {
      try {
        const features = geoJsonFormat.readFeatures(JSON.parse(finalLine))
        if (features && features.length > 0) {
          vectorSource.addFeatures(features)
        }
      } catch {
        // ignore trailing partial JSON
      }
    }
  })().catch(error => console.error(`[${targetElementId}] Stream failed`, error))

  return {
    map: mapInstance,
    source: vectorSource,
    stop: () => abortController.abort()
  }
}
