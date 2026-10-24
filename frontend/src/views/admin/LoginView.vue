<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'
const auth = useAuth(),
  router = useRouter(),
  username = ref('admin'),
  password = ref(''),
  busy = ref(false),
  error = ref('')
async function login() {
  busy.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    password.value = ''
    router.replace('/admin')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <section class="shell section">
    <form class="admin-login" @submit.prevent="login">
      <p class="eyebrow">F6S RETAIL WORKSPACE</p>
      <h1 class="text-3xl font-semibold my-5">Đăng nhập quản trị</h1>
      <p class="text-stone-500 text-sm mb-6">
        Dùng ADMIN_USERNAME / ADMIN_PASSWORD của backend mới, không phải tài khoản MySQL cũ.
      </p>
      <label class="field"
        >Tài khoản<input
          v-model.trim="username"
          required
          maxlength="100"
          autocomplete="username" /></label
      ><label class="field"
        >Mật khẩu<input v-model="password" required type="password" autocomplete="current-password"
      /></label>
      <p v-if="error" class="error-box" role="alert">{{ error }}</p>
      <button class="button w-full mt-5" :disabled="busy">
        {{ busy ? 'Đang xác thực…' : 'Đăng nhập' }}
      </button>
      <p class="text-xs text-stone-500 mt-4">
        Thông tin đăng nhập chỉ giữ trong bộ nhớ; tải lại trang sẽ cần đăng nhập lại.
      </p>
    </form>
  </section>
</template>
