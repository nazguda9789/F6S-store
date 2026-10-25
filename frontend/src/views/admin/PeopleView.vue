<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { api } from '../../lib/api'
const props = defineProps({ kind: { type: String, required: true } }),
  staff = computed(() => props.kind === 'staff')
const list = ref({ content: [], page: 0, totalPages: 0 }),
  busy = ref(false),
  error = ref(''),
  showForm = ref(false),
  editing = ref(null),
  selected = ref(null),
  addressId = ref(null),
  passwordTarget = ref(null),
  passwordDialog = ref(null),
  newPassword = ref('')
const filters = reactive({ q: '', active: '' }),
  profile = reactive({
    fullName: '',
    email: '',
    phone: '',
    gender: 'OTHER',
    birthDate: '',
    active: true,
  }),
  extra = reactive({ username: '', password: '', address: '' }),
  address = reactive({
    recipientName: '',
    phone: '',
    province: '',
    district: '',
    ward: '',
    detail: '',
    defaultAddress: false,
  })
async function run(work) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await work()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
async function load(page = 0) {
  list.value = await api('/admin/' + props.kind + '?' + new URLSearchParams({ ...filters, page }))
}
function reset() {
  editing.value = null
  showForm.value = true
  Object.assign(profile, {
    fullName: '',
    email: '',
    phone: '',
    gender: 'OTHER',
    birthDate: '',
    active: true,
  })
  Object.assign(extra, { username: '', password: '', address: '' })
}
function closeForm() {
  showForm.value = false
  extra.password = ''
}
function closePassword() {
  passwordDialog.value.close()
  newPassword.value = ''
}
function edit(row) {
  editing.value = row.id
  showForm.value = true
  Object.assign(profile, {
    ...row.profile,
    birthDate: row.profile.birthDate || '',
    email: row.profile.email || '',
    gender: row.profile.gender || 'OTHER',
  })
  Object.assign(extra, { username: row.username || '', password: '', address: row.address || '' })
}
async function save() {
  await run(async () => {
    const data = { ...profile, birthDate: profile.birthDate || null, email: profile.email || null }
    const body = staff.value
      ? {
          profile: data,
          address: extra.address,
          ...(!editing.value ? { username: extra.username, password: extra.password } : {}),
        }
      : data
    await api('/admin/' + props.kind + (editing.value ? '/' + editing.value : ''), {
      method: editing.value ? 'PUT' : 'POST',
      body: JSON.stringify(body),
    })
    extra.password = ''
    showForm.value = false
    await load(list.value.page)
  })
}
async function disable(row) {
  if (!confirm('Ngừng hoạt động ' + row.profile.fullName + '? Lịch sử vẫn được giữ.')) return
  await run(async () => {
    await api('/admin/' + props.kind + '/' + row.id, { method: 'DELETE' })
    await load(list.value.page)
  })
}
function resetPassword(row) {
  passwordTarget.value = row
  newPassword.value = ''
  passwordDialog.value.showModal()
}
async function password() {
  await run(async () => {
    await api('/admin/staff/' + passwordTarget.value.id + '/password', {
      method: 'PUT',
      body: JSON.stringify({ password: newPassword.value }),
    })
    newPassword.value = ''
    passwordDialog.value.close()
  })
}
async function detail(row) {
  await run(async () => {
    selected.value = await api('/admin/customers/' + row.id)
    resetAddress()
  })
}
function resetAddress(row = null) {
  addressId.value = row?.id ?? null
  Object.assign(
    address,
    row
      ? { ...row }
      : {
          recipientName: selected.value.profile.fullName,
          phone: selected.value.profile.phone,
          province: '',
          district: '',
          ward: '',
          detail: '',
          defaultAddress: !selected.value.addresses.length,
        },
  )
}
async function saveAddress() {
  await run(async () => {
    selected.value = await api(
      '/admin/customers/' +
        selected.value.id +
        '/addresses' +
        (addressId.value ? '/' + addressId.value : ''),
      {
        method: addressId.value ? 'PUT' : 'POST',
        body: JSON.stringify({
          recipientName: address.recipientName,
          phone: address.phone,
          province: address.province,
          district: address.district,
          ward: address.ward,
          detail: address.detail,
          defaultAddress: address.defaultAddress,
        }),
      },
    )
    resetAddress()
  })
}
async function deleteAddress(a) {
  if (!confirm('Ngừng dùng địa chỉ này?')) return
  await run(async () => {
    selected.value = await api('/admin/customers/' + selected.value.id + '/addresses/' + a.id, {
      method: 'DELETE',
    })
    resetAddress()
  })
}
watch(
  () => props.kind,
  () => {
    showForm.value = false
    selected.value = null
    extra.password = ''
    filters.q = ''
    filters.active = ''
    run(() => load())
  },
  { immediate: true },
)
</script>
<template>
  <section>
    <div class="admin-heading">
      <div>
        <p class="eyebrow">ACCOUNTS / {{ staff ? 'EMPLOYEES' : 'CUSTOMERS' }}</p>
        <h1>{{ staff ? 'Quản lý nhân viên' : 'Quản lý khách hàng' }}</h1>
      </div>
      <button class="button" :disabled="busy" @click="reset">
        + Thêm {{ staff ? 'nhân viên' : 'khách hàng' }}
      </button>
    </div>
    <p v-if="error" role="alert" class="error-box mb-5">{{ error }}</p>
    <form v-if="showForm" class="admin-card mb-6" @submit.prevent="save">
      <h2 class="text-xl font-semibold mb-5">{{ editing ? 'Cập nhật hồ sơ' : 'Tạo hồ sơ mới' }}</h2>
      <fieldset :disabled="busy">
        <div class="admin-form-grid">
          <label class="field"
            >Họ và tên<input v-model.trim="profile.fullName" maxlength="120" required /></label
          ><label class="field"
            >Số điện thoại<input
              v-model.trim="profile.phone"
              pattern="0[35789][0-9]{8}"
              maxlength="10"
              required /></label
          ><label class="field"
            >Email<input
              v-model.trim="profile.email"
              type="email"
              maxlength="254"
              :required="staff" /></label
          ><label class="field"
            >Ngày sinh<input
              v-model="profile.birthDate"
              type="date"
              :max="new Date(Date.now() - 86400000).toISOString().slice(0, 10)" /></label
          ><label class="field"
            >Giới tính<select v-model="profile.gender">
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác / Không cung cấp</option>
            </select></label
          ><label class="field"
            >Trạng thái<select v-model="profile.active">
              <option :value="true">Hoạt động</option>
              <option :value="false">Ngừng hoạt động</option>
            </select></label
          ><template v-if="staff"
            ><label class="field"
              >Tên đăng nhập<input
                v-model.trim="extra.username"
                :readonly="!!editing"
                pattern="[A-Za-z0-9._-]{3,50}"
                required /></label
            ><label v-if="!editing" class="field"
              >Mật khẩu khởi tạo<input
                v-model="extra.password"
                type="password"
                minlength="12"
                maxlength="72"
                autocomplete="new-password"
                required /></label
            ><label class="field"
              >Địa chỉ liên hệ<textarea
                v-model.trim="extra.address"
                maxlength="500"
                rows="2"
              /></label
          ></template>
        </div>
        <p v-if="staff" class="text-xs text-stone-500 mb-4">
          Nhân viên có quyền POS và hồ sơ khách hàng; không được quản lý tài khoản, giá, khuyến mãi
          hoặc báo cáo. Mật khẩu không gửi qua email tự động, hãy bàn giao an toàn ngoài hệ thống.
        </p>
        <button class="button">Lưu hồ sơ</button
        ><button type="button" class="button button-outline ml-3" @click="closeForm">Đóng</button>
      </fieldset>
    </form>
    <form
      class="admin-card flex flex-wrap gap-4 items-end mb-5"
      @submit.prevent="run(() => load())"
    >
      <label class="field flex-1 mb-0"
        >Tìm theo tên, mã, email hoặc số điện thoại<input
          v-model.trim="filters.q"
          maxlength="120" /></label
      ><label class="field mb-0"
        >Lọc trạng thái<select v-model="filters.active">
          <option value="">Tất cả</option>
          <option value="true">Hoạt động</option>
          <option value="false">Ngừng hoạt động</option>
        </select></label
      ><button class="button" :disabled="busy">Tìm kiếm</button>
    </form>
    <div class="admin-card table-scroll">
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã / Họ tên</th>
            <th>{{ staff ? 'Tài khoản' : 'Liên hệ' }}</th>
            <th>Ngày sinh / Giới tính</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list.content" :key="row.id">
            <td>
              <strong>{{ row.profile.fullName }}</strong
              ><small>{{ row.code }}</small>
            </td>
            <td>
              {{ staff ? row.username : row.profile.phone }}<small>{{ row.profile.email }}</small>
            </td>
            <td>
              {{ row.profile.birthDate || '—'
              }}<small>{{
                row.profile.gender === 'MALE'
                  ? 'Nam'
                  : row.profile.gender === 'FEMALE'
                    ? 'Nữ'
                    : 'Khác / Không cung cấp'
              }}</small>
            </td>
            <td>
              <span class="status-pill">{{
                row.profile.active ? 'Hoạt động' : 'Ngừng hoạt động'
              }}</span>
            </td>
            <td>
              <div class="flex gap-3 flex-wrap">
                <button class="text-link" :disabled="busy" @click="edit(row)">Sửa</button
                ><button
                  v-if="staff"
                  class="text-link"
                  :disabled="busy"
                  @click="resetPassword(row)"
                >
                  Đổi mật khẩu</button
                ><button v-else class="text-link" :disabled="busy" @click="detail(row)">
                  Địa chỉ</button
                ><button
                  class="text-red-700"
                  :disabled="busy || !row.profile.active"
                  @click="disable(row)"
                >
                  Ngừng
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!list.content.length" class="empty-state">
        Chưa có hồ sơ phù hợp. Không tự tạo nhân viên/khách hàng giả.
      </p>
      <div v-if="list.totalPages > 1" class="pagination">
        <button :disabled="busy || !list.page" @click="run(() => load(list.page - 1))">Trước</button
        ><span>{{ list.page + 1 }} / {{ list.totalPages }}</span
        ><button
          :disabled="busy || list.page + 1 >= list.totalPages"
          @click="run(() => load(list.page + 1))"
        >
          Sau
        </button>
      </div>
    </div>
    <section v-if="selected && !staff" class="admin-card mt-6">
      <div class="flex justify-between items-center mb-5">
        <h2 class="text-xl font-semibold">Địa chỉ của {{ selected.profile.fullName }}</h2>
        <button class="text-link" @click="selected = null">Đóng</button>
      </div>
      <div class="address-cards">
        <article v-for="a in selected.addresses" :key="a.id">
          <span v-if="a.defaultAddress" class="status-pill">Mặc định</span
          ><strong>{{ a.recipientName }} · {{ a.phone }}</strong>
          <p>{{ a.fullAddress }}</p>
          <div class="flex gap-4 mt-3">
            <button class="text-link" @click="resetAddress(a)">Sửa địa chỉ</button
            ><button class="text-red-700 text-sm" :disabled="busy" @click="deleteAddress(a)">
              Ngừng dùng
            </button>
          </div>
        </article>
      </div>
      <form class="mt-6 border-t pt-5" @submit.prevent="saveAddress">
        <h3 class="font-semibold mb-4">{{ addressId ? 'Sửa địa chỉ' : 'Thêm địa chỉ' }}</h3>
        <fieldset :disabled="busy || !selected.profile.active">
          <div class="admin-form-grid">
            <label class="field"
              >Tên người nhận<input
                v-model.trim="address.recipientName"
                required
                maxlength="120" /></label
            ><label class="field"
              >Điện thoại nhận hàng<input
                v-model.trim="address.phone"
                required
                pattern="0[35789][0-9]{8}"
                maxlength="10" /></label
            ><label class="field"
              >Tỉnh / Thành phố<input
                v-model.trim="address.province"
                required
                maxlength="100" /></label
            ><label class="field"
              >Quận / Huyện<input v-model.trim="address.district" required maxlength="100" /></label
            ><label class="field"
              >Phường / Xã<input v-model.trim="address.ward" required maxlength="100" /></label
            ><label class="field"
              >Số nhà / Đường<input v-model.trim="address.detail" required maxlength="250"
            /></label>
          </div>
          <label class="text-sm flex gap-2 mb-4"
            ><input v-model="address.defaultAddress" type="checkbox" />Địa chỉ mặc định</label
          ><button class="button">Lưu địa chỉ</button
          ><button type="button" class="button button-outline ml-3" @click="resetAddress()">
            Nhập địa chỉ mới
          </button>
        </fieldset>
      </form>
    </section>
    <dialog ref="passwordDialog" class="qr-label-dialog">
      <form @submit.prevent="password">
        <h2 class="text-xl font-semibold mb-5">Đặt lại mật khẩu {{ passwordTarget?.username }}</h2>
        <label class="field"
          >Mật khẩu mới<input
            v-model="newPassword"
            type="password"
            required
            minlength="12"
            maxlength="72"
            autocomplete="new-password"
        /></label>
        <div class="flex gap-3">
          <button class="button" :disabled="busy">Lưu mật khẩu</button
          ><button type="button" class="button button-outline" @click="closePassword">Đóng</button>
        </div>
      </form>
    </dialog>
  </section>
</template>
