import type {ReactNode} from 'react';
import {CountUp} from '@/components/CountUp';
import styles from './spas.less';

export interface StatFilterItem {
    key: string;
    label: string;
    /** 统计值，缺省显示占位符 */
    value?: number;
    icon?: ReactNode;
    /** 图标前景色 */
    color?: string;
    /** 图标底色 */
    background?: string;
    /** 数值后的补充说明 */
    sub?: string;
    /** 是否可点击切换过滤，缺省 true */
    clickable?: boolean;
}

interface StatFilterBarProps {
    items: StatFilterItem[];
    /** 当前选中的 key（不可过滤时无选中态） */
    activeKey?: string;
    onChange?: (key: string) => void;
}

/**
 * 顶部「统计 + 过滤」胶囊卡条：左侧彩色图标、右侧标签与滚动数值。
 * 可点击项作为状态筛选入口（选中态为渐变底），纯统计项保持静态外观。
 */
export function StatFilterBar({items, activeKey, onChange}: StatFilterBarProps) {
    return (
        <div className={styles.panel}>
            <div className={styles.filterBar}>
                {items.map(item => {
                    const clickable = item.clickable !== false;
                    const active = clickable && item.key === activeKey;
                    return (
                        <button
                            key={item.key}
                            type="button"
                            aria-pressed={clickable ? active : undefined}
                            className={[
                                styles.filterCard,
                                clickable ? '' : styles.filterStatic,
                                active ? styles.filterActive : ''
                            ].filter(Boolean).join(' ')}
                            onClick={() => {
                                if (clickable && !active) onChange?.(item.key);
                            }}
                        >
                            <span className={styles.filterIcon} style={{background: item.background, color: item.color}}>
                                {item.icon}
                            </span>
                            <span className={styles.filterBody}>
                                <span className={styles.filterLabel}>{item.label}</span>
                                <span className={styles.filterValue}>
                                    {item.value == null ? '-' : <CountUp value={item.value}/>}
                                    {item.sub ? <span className={styles.filterSub}>{item.sub}</span> : null}
                                </span>
                            </span>
                        </button>
                    );
                })}
            </div>
        </div>
    );
}

export default StatFilterBar;
