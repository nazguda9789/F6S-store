<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
const auth = useAuth(),
  router = useRouter(),
  username = ref(''),
  showPassword = ref(false),
  password = ref(''),
  busy = ref(false),
  error = ref('')
async function login() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    password.value = ''
    router.replace(auth.isAdmin ? '/admin' : '/admin/pos')
  } catch (e) {
    error.value = e.message
  } finally {
    password.value = ''
    busy.value = false
  }
}
</script>
<template>
  <section class="admin-card">
    <div v-if="auth.authenticated">
      <h2 class="text-xl font-semibold mb-4">Bạn đã đăng nhập: {{ auth.username }}</h2>
      <RouterLink :to="auth.isAdmin ? '/admin' : '/admin/pos'" class="button">{{
        auth.isAdmin ? 'Vào trang quản trị' : 'Vào bán hàng tại quầy'
      }}</RouterLink>
      <button class="button button-outline ml-3" @click="auth.logout()">
        Đổi tài khoản làm việc
      </button>
    </div>
    <form v-else @submit.prevent="login">
      <p class="eyebrow">F6S RETAIL WORKSPACE</p>
      <h2 class="text-2xl font-semibold my-5">Đăng nhập nhân viên / quản trị</h2>
      <p class="text-stone-500 text-sm mb-6">
        Dùng tài khoản được cửa hàng cấp. Hệ thống tự xác định quyền; bạn không cần chọn admin hay
        nhân viên.
      </p>
      <fieldset :disabled="busy">
        <label class="field"
          >Tài khoản<input
            v-model.trim="username"
            required
            maxlength="100"
            autocomplete="username" /></label
        ><label class="field"
          >Mật khẩu<input
            v-model="password"
            required
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
        /></label>
        <button
          type="button"
          class="text-link mb-4"
          :aria-pressed="showPassword"
          @click="showPassword = !showPassword"
        >
          {{ showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu' }}
        </button>
        <p v-if="error" class="error-box" role="alert">{{ error }}</p>
        <button class="button w-full mt-5" :disabled="busy">
          {{ busy ? 'Đang xác thực…' : 'Đăng nhập' }}
        </button>
      </fieldset>
      <p class="text-sm text-stone-500 mt-4">
        Quên mật khẩu nhân viên? Liên hệ quản trị cửa hàng để đặt lại. Khách mua hàng chọn mục
        “Khách hàng” phía trên.
      </p>
      <p class="text-xs text-stone-500 mt-4">
        Thông tin đăng nhập chỉ giữ trong bộ nhớ; tải lại trang sẽ cần đăng nhập lại.
      </p>
    </form>
  </section>
</template>
