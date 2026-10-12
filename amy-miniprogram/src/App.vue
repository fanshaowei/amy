<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

onLaunch(() => {
  // 启动时恢复登录态
  const userStore = useUserStore()
  userStore.restore()
  console.log('App Launch')
})
</script>

<style lang="scss">
/* 全局样式 */
page {
  background-color: #f4f6fa;
  color: #1f2329;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue',
    'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  line-height: 1.6;
}

/* 通用工具类 */
.flex {
  display: flex;
}
.flex-col {
  display: flex;
  flex-direction: column;
}
.flex-1 {
  flex: 1;
  min-width: 0;
}
.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}
.items-center {
  align-items: center;
}
.justify-between {
  justify-content: space-between;
}
.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
