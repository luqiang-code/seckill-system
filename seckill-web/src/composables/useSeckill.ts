import { ref, onUnmounted, toValue, type MaybeRefOrGetter } from 'vue'
import { doSeckill, fetchResult, payOrder, ApiError } from '../api'
import type { OrderInfo } from '../api/types'
import { PAYMENT_DEADLINE_MS, DEBOUNCE_MS } from '../api/constants'

export function useSeckill(goodsId: MaybeRefOrGetter<number>) {
  const resultType = ref<'success' | 'fail' | 'info' | ''>('')
  const resultMsg = ref('')
  const orderInfo = ref<OrderInfo | null>(null)
  const paymentLeft = ref(0)
  let cooldownUntil = 0
  let paymentTimer: ReturnType<typeof setInterval> | null = null

  function startPaymentCountdown() {
    if (!orderInfo.value) return
    const deadline = new Date(orderInfo.value.createTime).getTime() + PAYMENT_DEADLINE_MS
    stopPaymentCountdown()
    paymentTimer = setInterval(() => {
      const remain = Math.max(0, Math.floor((deadline - Date.now()) / 1000))
      paymentLeft.value = remain
      if (remain <= 0) {
        stopPaymentCountdown()
        resultType.value = 'fail'
        resultMsg.value = '订单已超时取消'
        orderInfo.value = null
      }
    }, 200)
  }

  function stopPaymentCountdown() {
    if (paymentTimer) {
      clearInterval(paymentTimer)
      paymentTimer = null
    }
  }

  function pollResult(id: number) {
    const check = () => {
      fetchResult(id)
        .then(order => {
          orderInfo.value = order
          resultMsg.value = '订单号: ' + order.id
          startPaymentCountdown()
        })
        .catch(err => {
          if (err instanceof ApiError && err.code === 3) setTimeout(check, 500)
        })
    }
    setTimeout(check, 300)
  }

  async function handleSeckill(onSuccess?: () => void) {
    const now = Date.now()
    if (now < cooldownUntil) return
    cooldownUntil = now + DEBOUNCE_MS
    clearResult()

    const id = toValue(goodsId)

    try {
      await doSeckill(id)
      resultType.value = 'success'
      resultMsg.value = '抢到了！'
      pollResult(id)
      onSuccess?.()
    } catch (e) {
      if (e instanceof ApiError) {
        switch (e.code) {
          case 429: resultType.value = 'info'; resultMsg.value = '请求太频繁'; break
          case 2:   resultType.value = 'info'; resultMsg.value = '已抢过'; break
          case 0:   resultType.value = 'fail'; resultMsg.value = '已售罄'; break
          default:  resultType.value = 'fail'; resultMsg.value = e.message
        }
      } else {
        resultType.value = 'fail'
        resultMsg.value = '网络错误'
      }
    }
  }

  async function handlePay() {
    if (!orderInfo.value) return
    try {
      await payOrder(orderInfo.value.id)
      resultType.value = 'success'
      resultMsg.value = '支付成功！订单号: ' + (orderInfo.value.id ?? '')
      stopPaymentCountdown()
      paymentLeft.value = 0
    } catch (e) {
      resultType.value = 'fail'
      resultMsg.value = e instanceof ApiError ? e.message : '支付失败'
    }
  }

  function clearResult() {
    resultType.value = ''
    resultMsg.value = ''
    orderInfo.value = null
    paymentLeft.value = 0
    stopPaymentCountdown()
  }

  onUnmounted(() => stopPaymentCountdown())

  return {
    resultType,
    resultMsg,
    orderInfo,
    paymentLeft,
    handleSeckill,
    handlePay,
    clearResult,
  }
}
