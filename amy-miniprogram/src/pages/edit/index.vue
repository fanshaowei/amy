<script setup lang="ts">
import PageHero from '@/components/PageHero.vue'
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { isLocalTempFile, uploadAvatar } from '@/api/user'
import { showError } from '@/utils/request'
import { requireLogin } from '@/utils/authGuard'

const userStore = useUserStore()

/** 兼容旧的「首次登录后跳编辑资料」入口；当前登录流程已不再使用该参数 */
const firstTime = ref(false)
const nickname = ref('')
/** 当前待提交的头像：未换头像时是服务端的持久化 URL，重新选择后是微信临时路径 */
const avatarUrl = ref('')
const saving = ref(false)

const defaultAvatar = 'https://i.pravatar.cc/120?img=12'

onLoad((query) => {
  firstTime.value = query?.firstTime === '1'
})

onShow(async () => {
  // 未登录 → 授权登录页；从登录页放弃授权返回 → 统一回首页
  if (!requireLogin()) return
  try {
    const user = await userStore.fetchProfile(true) // 无感刷新，失败不弹提示
    if (user) {
      // 仅在未填写时回填，避免覆盖用户正在编辑的内容；
      // 回填的 avatarImgUrl 是文件服务的持久化地址，未换头像时保存不再上传
      if (!nickname.value) nickname.value = user.nickname || ''
      if (!avatarUrl.value) avatarUrl.value = user.avatarImgUrl || ''
    }
  } catch (e) {
    // 刷新失败不阻塞页面展示
  }
})

/**
 * 选择微信头像（open-type="chooseAvatar"）。
 * 拿到的是 wxfile:// / http://tmp/ 临时路径，仅本次会话有效，
 * 保存时必须先上传文件服务换成持久化 URL（见 onSave）。
 */
function onChooseAvatar(e: any) {
  const url = e?.detail?.avatarUrl
  if (url) avatarUrl.value = url
}

/**
 * 保存头像/昵称。
 * 只有本次「重新选择了微信头像」才会上传文件服务并更新头像访问地址；
 * 未换头像时直接复用服务端已有的持久化 URL，不产生重复上传。
 */
async function onSave() {
  if (saving.value) return
  const nick = nickname.value.trim()
  if (!nick) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  // 未换头像时 avatarUrl 由 onShow 回填为服务端持久地址；再兜一层 store 避免空值
  const current = avatarUrl.value || userStore.avatar
  if (!current) {
    uni.showToast({ title: '请选择头像', icon: 'none' })
    return
  }
  saving.value = true
  try {
    let avatar = current
    if (isLocalTempFile(avatar)) {
      avatar = await uploadAvatar(avatar)
      avatarUrl.value = avatar
    }
    await userStore.updateProfile(nick, avatar)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => {
      if (firstTime.value) {
        // 首次登录完成资料编辑后进首页
        uni.switchTab({ url: '/pages/home/index' })
      } else {
        uni.navigateBack()
      }
    }, 600)
  } catch (e) {
    showError(e, '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view class="edit">
    <PageHero title="编辑资料" eyebrow="PROFILE · 个人资料" subtitle="完善您的昵称与头像" compact flush />
    <view class="edit__avatar">
      <!-- #ifdef MP-WEIXIN -->
      <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
        <image class="avatar" :src="avatarUrl || userStore.avatar || defaultAvatar" mode="aspectFill" />
        <view class="avatar-badge">改</view>
      </button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <image class="avatar" :src="avatarUrl || userStore.avatar || defaultAvatar" mode="aspectFill" />
      <!-- #endif -->
      <text class="hint">点击头像更换</text>
    </view>

    <view class="form">
      <view class="item">
        <text class="lbl">昵称</text>
        <input placeholder-class="art-ph" class="val" v-model="nickname" type="nickname" placeholder="请输入昵称" />
      </view>
      <view class="item">
        <text class="lbl">手机</text>
        <text class="val">{{ userStore.phone || '未绑定' }}</text>
      </view>
    </view>

    <view class="save-btn" :class="{ disabled: saving }" @tap="onSave">
      {{ saving ? '保存中...' : '保存' }}
    </view>
  </view>
</template>

<style scoped lang="scss">
.edit {
  min-height: 100vh;
  padding: 0 24rpx $art-page-bottom;
}
.edit__avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $art-gap 0;
}
.avatar-btn {
  position: relative;
  width: 140rpx;
  height: 140rpx;
  padding: 0;
  margin: 0;
  background: transparent;
  border: none;
  line-height: 1;
}
.avatar-btn::after {
  border: none;
}
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 3rpx solid rgba(240, 207, 148, 0.62);
  box-shadow: 0 8rpx 24rpx rgba(28, 44, 90, 0.18);
}
.avatar-badge {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $art-accent;
  color: $art-on-gold;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hint {
  font-size: 24rpx;
  color: $uni-text-color-grey;
  margin-top: 16rpx;
}
.form {
  @include art-card(0.6, 18px, 24rpx);
  padding: 0 32rpx;
  margin-bottom: $art-gap;
}
.item {
  padding: 28rpx 0;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 1px solid $art-divider;
}
.item:last-child {
  border-bottom: 0;
}
.lbl {
  width: 100rpx;
  font-size: 26rpx;
  color: $art-text;
}
.val {
  flex: 1;
  font-size: 28rpx;
  color: $art-text-strong;
}
.save-btn {
  @include art-gold-button();
  
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
}
.save-btn.disabled {
  opacity: 0.6;
}
</style>
