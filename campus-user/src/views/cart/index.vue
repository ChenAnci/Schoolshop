<template>
  <div class="cart-page">
    <h2 class="page-title">购物车</h2>

    <el-empty v-if="!cartStore.loaded" description="加载中..." />
    <el-empty v-else-if="cartStore.items.length === 0" description="购物车还是空的，去逛逛吧">
      <el-button type="primary" @click="router.push('/product/list')">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <el-alert
        v-if="hasOffShelf"
        title="部分商品已下架，下单前请先移除"
        type="warning"
        :closable="false"
        show-icon
        class="off-shelf-tip"
      />
      <el-card shadow="never" class="cart-card">
        <el-table :data="cartStore.items">
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div class="cart-item">
                <img
                  :src="productImageOf({ productName: row.productName, productImage: row.productImage })"
                  :alt="row.productName"
                  class="cart-item-img"
                />
                <div>
                  <div class="cart-item-name">{{ row.productName }}</div>
                  <div class="cart-item-shop">{{ row.shopName }}</div>
                  <el-tag v-if="row.productStatus !== '0'" type="danger" size="small">已下架</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="140">
            <template #default="{ row }">
              <span class="price">￥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                size="small"
                @change="(val: number | undefined) => cartStore.updateQuantity(row.cartId, val ?? 1)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="140">
            <template #default="{ row }">
              <span class="subtotal">￥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleRemove(row.cartId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="cart-footer">
          <el-button @click="handleClear">清空购物车</el-button>
          <div class="cart-summary">
            <span>共 <b>{{ cartStore.count }}</b> 件，合计：</span>
            <span class="total">￥{{ cartStore.totalPrice.toFixed(2) }}</span>
            <el-button type="primary" size="large" :icon="Check" @click="handleCheckout">
              去结算
            </el-button>
          </div>
        </div>
      </el-card>

      <el-dialog v-model="checkoutVisible" title="确认订单信息" width="560px">
        <el-form label-width="90px">
          <el-form-item label="取货人" required>
            <el-input v-model="receiverName" placeholder="请输入取货人姓名" />
          </el-form-item>
          <el-form-item label="联系电话" required>
            <el-input v-model="receiverPhone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="取货地址">
            <el-input v-model="receiverAddress" placeholder="线下自提 / 当面交易地点（选填）" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="remark" type="textarea" :rows="2" placeholder="给商家的留言（选填）" />
          </el-form-item>
        </el-form>
        <div class="checkout-total">
          共 <b>{{ cartStore.count }}</b> 件，合计：
          <span class="total">￥{{ cartStore.totalPrice.toFixed(2) }}</span>
        </div>
        <template #footer>
          <el-button @click="checkoutVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitCheckout">提交订单</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { productImageOf, createOrder, type CartItem } from '@/api/market'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const checkoutVisible = ref(false)
const submitting = ref(false)
const receiverName = ref('')
const receiverPhone = ref('')
const receiverAddress = ref('')
const remark = ref('')

const hasOffShelf = computed(() => cartStore.items.some((i) => i.productStatus !== '0'))

async function load() {
  if (!userStore.token) {
    router.replace('/login')
    return
  }
  try {
    await cartStore.fetch()
  } catch {
    // 请求层已提示
  }
}

function handleRemove(cartId: number) {
  ElMessageBox.confirm('确定删除该商品吗？', '提示', { type: 'warning' })
    .then(async () => {
      await cartStore.remove([cartId])
      ElMessage.success('已删除')
    })
    .catch(() => undefined)
}

function handleClear() {
  ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
    .then(async () => {
      await cartStore.clear()
      ElMessage.success('已清空')
    })
    .catch(() => undefined)
}

const clearValid = computed(() => cartStore.items.every((i) => i.productStatus === '0'))

function handleCheckout() {
  if (cartStore.items.length === 0) return
  if (!clearValid.value) {
    ElMessage.warning('存在已下架商品，请先移除后再结算')
    return
  }
  const user = userStore.userInfo as Record<string, unknown> | null
  receiverName.value = (user?.nickName as string) || ''
  receiverPhone.value = ''
  receiverAddress.value = ''
  remark.value = ''
  checkoutVisible.value = true
}

async function submitCheckout() {
  if (!receiverName.value.trim()) {
    ElMessage.warning('请填写取货人姓名')
    return
  }
  if (!receiverPhone.value.trim()) {
    ElMessage.warning('请填写联系电话')
    return
  }
  submitting.value = true
  try {
    const cartIds = cartStore.items.map((i: CartItem) => i.cartId)
    const res = await createOrder({
      cartIds,
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      receiverAddress: receiverAddress.value,
      remark: remark.value
    })
    ElMessage.success('下单成功')
    checkoutVisible.value = false
    cartStore.items = []
    cartStore.loaded = false
    router.push('/order/list')
    void res
  } catch {
    // 请求层已提示
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.cart-page {
  width: 1200px;
  margin: 0 auto;
  padding: 16px 0 32px;
}
.page-title {
  font-size: 20px;
  color: #333;
  margin: 0 0 14px;
}
.off-shelf-tip {
  margin-bottom: 12px;
}
.cart-card {
  border-radius: 8px;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cart-item-img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f6f8;
}
.cart-item-name {
  color: #333;
  font-size: 14px;
}
.cart-item-shop {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
.price {
  color: #ff5000;
  font-size: 15px;
}
.subtotal {
  color: #ff5000;
  font-size: 16px;
  font-weight: 600;
}
.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.cart-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #555;
  font-size: 14px;
}
.cart-summary b {
  color: #ff5000;
}
.total {
  color: #ff5000;
  font-size: 22px;
  font-weight: 700;
}
.checkout-total {
  padding: 0 8px 4px;
  text-align: right;
  color: #555;
  font-size: 14px;
}
</style>