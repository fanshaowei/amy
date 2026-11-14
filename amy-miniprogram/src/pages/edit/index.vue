<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { showError } from '@/utils/request'

const userStore = useUserStore()

/** 首次登录流程：保存成功后直接进首页 */
const firstTime = ref(false)
const nickname = ref('')
const avatarUrl = ref('')
const saving = ref(false)
const binding = ref(false)

const defaultAvatar = 'https://i.pravatar.cc/120?img=12'

onLoad((query) => {
  firstTime.value = query?.firstTime === '1'
})

onShow(async () => {
  if (!userStore.loggedIn) return
  try {
    const user = await userStore.fetchProfile(true) // 无感刷新，失败不弹提示
    if (user) {
      // 仅在未填写时回填，避免覆盖用户正在编辑的内容
      if (!nickname.value) nickname.value = user.nickname || ''
      if (!avatarUrl.value) avatarUrl.value = user.avatarImgUrl || ''
    }
  } catch (e) {
    // 刷新失败不阻塞页面展示
  }
})

/** 选择微信头像（open-type="chooseAvatar"） */
function onChooseAvatar(e: any) {
  const url = e?.detail?.avatarUrl
  if (url) avatarUrl.value = url
}

/** 保存头像/昵称 */
async function onSave() {
  if (saving.value) return
  const nick = nickname.value.trim()
  if (!nick) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  if (!avatarUrl.value) {
    uni.showToast({ title: '请选择头像', icon: 'none' })
    return
  }
  saving.value = true
  try {
    let avatar = avatarUrl.value
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

/** 微信手机号授权后绑定（open-type="getPhoneNumber"） */
async function onGetPhoneNumber(e: any) {
  const jsCode = e?.detail?.code
  if (!jsCode) {
    uni.showToast({ title: '已取消手机号授权', icon: 'none' })
    return
  }
  binding.value = true
  try {
    const phone = await userStore.updatePhone(jsCode)
    uni.showToast({ title: `已绑定 ${phone}`, icon: 'none' })
  } catch (err) {
    showError(err, '绑定失败')
  } finally {
    binding.value = false
  }
}
</script>

<template>
  <view class="edit">
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
        <input class="val" v-model="nickname" type="nickname" placeholder="请输入昵称" />
      </view>
      <view class="item">
        <text class="lbl">手机</text>
        <text class="val">{{ userStore.phone || '未绑定' }}</text>
      </view>
    </view>

    <view class="save-btn" :class="{ disabled: saving }" @tap="onSave">
      {{ saving ? '保存中...' : '保存' }}
    </view>

    <button
      v-if="!firstTime"
      class="phone-btn"
      :class="{ disabled: binding }"
      open-type="getPhoneNumber"
      @getphonenumber="onGetPhoneNumber"
    >
      {{ binding ? '绑定中...' : userStore.phone ? '更换手机号' : '微信一键绑定手机号' }}
    </button>
  </view>
</template>

<style scoped lang="scss">
.edit {
  min-height: 100vh;
  padding: 24rpx;
}
.edit__avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
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
  border: 4rpx solid #f2f3f5;
}
.avatar-badge {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #3b7dff;
  color: #fff;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hint {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 16rpx;
}
.form {
  background: #f7f8fa;
  border-radius: 24rpx;
  padding: 0 32rpx;
  margin-bottom: 32rpx;
}
.item {
  padding: 28rpx 0;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 1px solid #ecedf0;
}
.item:last-child {
  border-bottom: 0;
}
.lbl {
  width: 100rpx;
  font-size: 26rpx;
  color: #4e5969;
}
.val {
  flex: 1;
  font-size: 28rpx;
}
.save-btn {
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.save-btn.disabled {
  opacity: 0.6;
}
.phone-btn {
  background: #f2f3f5;
  color: #4e5969;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  margin-top: 24rpx;
}
.phone-btn.disabled {
  opacity: 0.6;
}
.phone-btn::after {
  border: none;
}
</style>
