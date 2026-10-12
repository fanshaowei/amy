<script setup lang="ts"></script>

<template>
  <view class="about">
    <view class="about__card">
      <view class="h">服务说明</view>
      <view class="p">本平台提供活动预约服务，用户可通过小程序浏览可预约活动、提交预约申请、查看预约记录。</view>

      <view class="h">预约规则</view>
      <view class="p">
        1. 用户需通过微信登录获取身份信息后方可预约；\n2. 同一身份证每月最多预约2次，超出将无法提交；\n3. 预约成功后请按时到店核销，逾期将自动过期；\n4. 详见各活动页面的预约须知。
      </view>

      <view class="h">客服联系</view>
      <view class="p">
        电话：400-888-8888\n邮箱：service@example.com\n工作时间：9:00-18:00
      </view>

      <view class="h">版本</view>
      <view class="p">v1.0.0</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.about {
  min-height: 100vh;
  padding: 24rpx;
}
.about__card {
  background: #fff;
  border-radius: 32rpx;
  padding: 40rpx;
  line-height: 1.9;
  font-size: 28rpx;
  color: #4e5969;
}
.h {
  color: #1f2329;
  font-size: 30rpx;
  font-weight: 600;
  margin: 28rpx 0 16rpx;
}
.h:first-child {
  margin-top: 0;
}
.p {
  white-space: pre-line;
}
</style>
