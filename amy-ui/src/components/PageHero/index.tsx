import {ReloadOutlined} from '@ant-design/icons';
import type {ReactNode} from 'react';
import styles from './index.less';

export interface PageHeroProps {
    /** 面包屑路径，如「艺术空间 › 项目管理」 */
    crumb: ReactNode;
    /** 页面标题 */
    title: string;
    /** 一句话说明 */
    description?: string;
    /** 右侧操作区 */
    extra?: ReactNode;
    /** 数据更新时间（HH:mm），传入后展示刷新胶囊 */
    updatedAt?: string;
    /** 是否处于刷新中 */
    loading?: boolean;
    onRefresh?: () => void;
}

/**
 * 艺术空间模块统一的渐变页头。
 * 视觉与控制台（Welcome）保持一致：渐变底 + 面包屑 + 标题 + 更新时间胶囊。
 * 内容区通过与 .content 的负边距上浮形成层次，页面自身不需要再写背景。
 */
export function PageHero({crumb, title, description, extra, updatedAt, loading, onRefresh}: PageHeroProps) {
    return (
        <div className={styles.hero}>
            <div className={styles.heroMain}>
                <div className={styles.heroText}>
                    <div className={styles.heroCrumb}>{crumb}</div>
                    <h2 className={styles.heroTitle}>{title}</h2>
                    {description ? <div className={styles.heroSub}>{description}</div> : null}
                </div>
                <div className={styles.heroActions}>
                    {updatedAt ? (
                        <button type="button" className={styles.refreshChip} onClick={onRefresh}
                                disabled={loading} aria-label="刷新数据">
                            <ReloadOutlined spin={loading}/>
                            数据更新于 {updatedAt}
                        </button>
                    ) : null}
                    {extra}
                </div>
            </div>
        </div>
    );
}

export default PageHero;
