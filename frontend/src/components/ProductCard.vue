<script setup>
import { money } from '../lib/api'
defineProps({ product: Object })
</script>
<template>
  <RouterLink :to="`/products/${product.id}`" class="product-card group">
    <div class="product-picture">
      <span v-if="!product.stock" class="badge">Hết hàng</span
      ><span v-else-if="product.featured" class="badge">F6S SELECT</span>
      <img :src="product.imageUrl" :alt="product.name" loading="lazy" class="primary-image" />
      <img
        v-if="product.hoverImageUrl"
        :src="product.hoverImageUrl"
        alt=""
        loading="lazy"
        class="hover-image"
      />
      <span class="view-product">Khám phá <ArrowIcon /></span>
    </div>
    <div class="flex justify-between gap-3 mt-4">
      <div>
        <p class="eyebrow">{{ product.brand }} / {{ product.category.name }}</p>
        <h3 class="mt-1 font-semibold">{{ product.name }}</h3>
      </div>
      <span class="text-sm font-semibold whitespace-nowrap">Từ {{ money(product.price) }}</span>
    </div>
    <p class="text-sm text-stone-500 mt-2">
      {{ product.colorways.length }} phối màu · {{ product.stock ? 'Sẵn hàng' : 'Tạm hết hàng' }}
    </p>
  </RouterLink>
</template>
