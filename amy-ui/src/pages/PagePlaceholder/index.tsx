import {PageContainer} from '@ant-design/pro-components';
import {useLocation} from '@umijs/max';

/**
 * 用于：
  / 后端路由里有但前端未实现具体页面的菜单（动态新增的菜单目录）。
  / config.ts 里登记为占位的顶级目录（如 /monitor/*、/tool/*）—— 真实页面未实现时统一提示。
 */
export default function PagePlaceholder() {
    const {pathname} = useLocation();
    return (
        <PageContainer
            header={{
                title: '页面未实现',
                breadcrumb: {}
            }}
        >
            <div style={{
                background: '#fff',
                padding: 24,
                borderRadius: 6,
                minHeight: 320,
                textAlign: 'center'
            }}>
                <h3 style={{color: '#888', marginBottom: 8}}>
                    「{pathname}」对应的前端页面尚未实现
                </h3>
                <p style={{color: '#aaa'}}>
                    该路径已由后端菜单管理录入并出现在侧边栏中，但前端的页面组件未注册。请在 <code>src/pages/</code> 下创建对应组件，并在 <code>config/config.ts</code> 的 <code>routes</code> 中将占位指向真实组件。
                </p>
            </div>
        </PageContainer>
    );
}