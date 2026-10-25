import type {ReactNode} from 'react';
import {CountUp} from '@/components/CountUp';
import styles from './index.less';

/** 环比方向 */
export type StatDeltaDirection = 'up' | 'down' | 'flat';

/** 指标卡片的环比徽章 */
export interface StatDelta {
    /** 方向：上升 / 下降 / 持平 */
    direction: StatDeltaDirection;
    /** 主文案，如 +8.2%、累计 */
    text: string;
    /** 补充说明，如 较昨日 */
    compare?: string;
}

export interface StatCardProps {
    /** 指标名称 */
    label: string;
    /** 指标数值 */
    value: number;
    /** 图标（@ant-design/icons 图标元素） */
    icon: ReactNode;
    /** 图标前景色 */
    color: string;
    /** 图标底色 */
    background: string;
    /** 环比徽章，缺省时不展示 */
    delta?: StatDelta;
    /** 是否处于加载状态 */
    loading?: boolean;
}

const ARROW: Record<StatDeltaDirection, string> = {up: '↑', down: '↓', flat: '•'};

const DELTA_CLASS: Record<StatDeltaDirection, string> = {
    up: 'deltaUp',
    down: 'deltaDown',
    flat: 'deltaFlat'
};

/** 控制台指标卡片：彩色图标 + 名称 + 滚动数字 + 环比徽章 */
export function StatCard({label, value, icon, color, background, delta, loading}: StatCardProps) {
    return (
        <div className={styles.statCard}>
            <div className={styles.statIcon} style={{background, color}}>
                {icon}
            </div>
            <div className={styles.statLabel}>{label}</div>
            {loading ? (
                <div className={styles.statValueLoading}/>
            ) : (
                <div className={styles.statValue}>
                    <CountUp value={value}/>
                </div>
            )}
            {delta ? (
                <div className={`${styles.statDelta} ${styles[DELTA_CLASS[delta.direction]] ?? ''}`}>
                    {ARROW[delta.direction]} {delta.text}
                    {delta.compare ? <span className={styles.statCompare}>{delta.compare}</span> : null}
                </div>
            ) : null}
        </div>
    );
}

export default StatCard;
