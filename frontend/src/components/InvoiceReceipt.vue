<script setup>
import { money } from '../lib/api'
defineProps({ invoice: Object })
function print() {
  window.print()
}
</script>
<template>
  <section v-if="invoice" class="invoice-receipt">
    <div class="flex justify-between items-center no-print mb-5">
      <h2 class="text-xl font-semibold">Hóa đơn đã ghi nhận</h2>
      <button class="button" @click="print">In hóa đơn</button>
    </div>
    <div class="receipt-paper">
      <h2 class="text-3xl font-black italic">F6S SPORTS</h2>
      <p class="text-sm">HÓA ĐƠN BÁN HÀNG · {{ invoice.channel }}</p>
      <p class="text-xs mt-2">
        Khách hàng: {{ invoice.customerName || 'Khách lẻ' }}
        <span v-if="invoice.phone">· {{ invoice.phone }}</span>
      </p>
      <p class="text-xs mt-3">
        {{ invoice.code }}<br />{{
          new Date(invoice.createdAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })
        }}
      </p>
      <table class="receipt-table">
        <thead>
          <tr>
            <th>Sản phẩm</th>
            <th>SL</th>
            <th>Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="i in invoice.items" :key="i.sku">
            <td>
              {{ i.productName }}<small>{{ i.color }} / {{ i.size }} · {{ i.sku }}</small
              ><small>{{ money(i.unitPrice) }} / đôi</small>
            </td>
            <td>{{ i.quantity }}</td>
            <td>{{ money(i.unitPrice * i.quantity) }}</td>
          </tr>
        </tbody>
      </table>
      <div class="receipt-totals">
        <p>
          <span>Tiền hàng</span><b>{{ money(invoice.subtotal) }}</b>
        </p>
        <p>
          <span>Giảm giá {{ invoice.promotionCode || '' }}</span
          ><b>− {{ money(invoice.discount) }}</b>
        </p>
        <p class="text-lg">
          <span>Thanh toán</span><b>{{ money(invoice.total) }}</b>
        </p>
        <template v-if="invoice.cashReceived != null"
          ><p>
            <span>Khách đưa</span><b>{{ money(invoice.cashReceived) }}</b>
          </p>
          <p>
            <span>Tiền thừa</span><b>{{ money(invoice.cashChange) }}</b>
          </p></template
        >
      </div>
      <p class="text-xs mt-5">
        Trạng thái: {{ invoice.status }} · {{ invoice.paymentMethod }}<br />Cảm ơn bạn đã mua sắm
        tại F6S.<br />Bản in bán hàng, không phải hóa đơn VAT.
      </p>
    </div>
  </section>
</template>
