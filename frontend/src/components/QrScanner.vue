<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { BrowserQRCodeReader } from '@zxing/browser'
const emit = defineEmits(['decoded']),
  video = ref(null),
  running = ref(false),
  error = ref('')
let controls,
  disposed = false
async function start() {
  error.value = ''
  running.value = true
  try {
    const reader = new BrowserQRCodeReader()
    const stream = await reader.decodeFromVideoDevice(undefined, video.value, (result) => {
      if (result && !disposed && running.value) {
        emit('decoded', result.getText())
        stop()
      }
    })
    if (disposed || !running.value) stream.stop()
    else controls = stream
  } catch (e) {
    running.value = false
    error.value =
      'Không mở được camera. Hãy cấp quyền, dùng HTTPS/localhost hoặc quét bằng máy quét USB.'
  }
}
function stop() {
  running.value = false
  controls?.stop()
  controls = null
  const stream = video.value?.srcObject
  stream?.getTracks?.().forEach((t) => t.stop())
}
onBeforeUnmount(() => {
  disposed = true
  stop()
})
</script>
<template>
  <div class="qr-scanner">
    <button type="button" class="button button-outline" @click="running ? stop() : start()">
      {{ running ? 'Tắt camera' : 'Quét QR bằng camera' }}</button
    ><video v-show="running" ref="video" muted playsinline aria-label="Camera quét mã QR" />
    <p v-if="error" role="alert" class="error-box mt-3">{{ error }}</p>
  </div>
</template>
