import {
    CalendarOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    DatabaseOutlined,
    DownloadOutlined,
    TeamOutlined,
    UserAddOutlined
} from '@ant-design/icons';
import {PageContainer} from '@ant-design/pro-components';
import {App, Button, Col, Row, Skeleton, Tooltip} from 'antd';
import type {ReactNode} from 'react';
import {useCallback, useEffect, useState} from 'react';
import LineChart from '@/components/LineChart';
import type {LineChartPoint} from '@/components/LineChart';
import {StatCard} from '@/components/StatCard';
import type {StatDelta} from '@/components/StatCard';
import {getDashboardStatistics} from '@/services/sunpalaceartspace/dashboard';
import type {DashboardStatistics, DashboardTrendItem} from '@/services/sunpalaceartspace/dashboard';
import styles from './index.less';

/** 可展示的数值型统计字段 */
type StatKey =
    | 'todayReservationCount'
    | 'todayVerifyCount'
    | 'tomorrowReservationCount'
    | 'totalReservationCount'
    | 'todayNewUserCount'
    | 'totalUserCount';

/** 环比数据所依赖的趋势字段 */
type TrendKey = 'reservation' | 'user';

interface StatItemConfig {
    key: StatKey;
    label: string;
    icon: ReactNode;
    /** 图标前景色 */
    color: string;
    /** 图标底色 */
    background: string;
    /** 累计类指标（展示「累计」徽章） */
    cumulative?: boolean;
    /** 环比数据来源趋势 */
    trend?: TrendKey;
}

const STAT_ITEMS: StatItemConfig[] = [
    {
        key: 'todayReservationCount',
        label: '今日预约人数',
        icon: <CalendarOutlined/>,
        color: '#4f6ef7',
        background: '#eef1fe',
        trend: 'reservation'
    },
    {
        key: 'todayVerifyCount',
        label: '今日核销人数',
        icon: <CheckCircleOutlined/>,
        color: '#10b981',
        background: '#e7f8f1'
    },
    {
        key: 'tomorrowReservationCount',
        label: '明日预约人数',
        icon: <ClockCircleOutlined/>,
        color: '#f59e0b',
        background: '#fef4e2'
    },
    {
        key: 'totalReservationCount',
        label: '预约总数',
        icon: <DatabaseOutlined/>,
        color: '#8b5cf6',
        background: '#f1ecfd',
        cumulative: true
    },
    {
        key: 'todayNewUserCount',
        label: '今日新增会员',
        icon: <UserAddOutlined/>,
        color: '#06b6d4',
        background: '#e5f8fb',
        trend: 'user'
    },
    {
        key: 'totalUserCount',
        label: '会员总数',
        icon: <TeamOutlined/>,
        color: '#ec4899',
        background: '#fdecf4',
        cumulative: true
    }
];

const CUMULATIVE_DELTA: StatDelta = {direction: 'flat', text: '累计'};

const RESERVATION_COLOR = '#4f6ef7';
const USER_COLOR = '#06b6d4';

const pad = (value: number) => String(value).padStart(2, '0');

const formatDate = (date: Date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const formatTime = (date: Date) => `${pad(date.getHours())}:${pad(date.getMinutes())}`;

/** 后端趋势数据 -> 图表数据（日期裁剪为 MM/DD） */
const toPoints = (list?: DashboardTrendItem[]): LineChartPoint[] =>
    (list || []).map(item => ({
        label: item.date ? item.date.slice(5).replace('-', '/') : '',
        title: item.date,
        value: item.count ?? 0
    }));

/** 取趋势数组倒数第 offset 项（offset = 0 表示今天） */
const trendValue = (list: DashboardTrendItem[] | undefined, offset: number): number | undefined => {
    if (!list || list.length <= offset) return undefined;
    return list[list.length - 1 - offset]?.count;
};

/** 由上期值计算环比徽章（缺少上期数据或上期为 0 时无法计算） */
const buildDelta = (current?: number, previous?: number): StatDelta | undefined => {
    if (current == null || previous == null || previous <= 0) return undefined;
    const rate = ((current - previous) / previous) * 100;
    return {
        direction: rate > 0 ? 'up' : rate < 0 ? 'down' : 'flat',
        text: `${rate > 0 ? '+' : ''}${rate.toFixed(1)}%`,
        compare: '较昨日'
    };
};

/** 导出趋势数据为 CSV */
const toCsv = (points: LineChartPoint[]) => ['日期,数量', ...points.map(p => `${p.title || p.label},${p.value}`)].join('\n');

/** 趋势图表卡片（标题 + 图例 + 下载 + 折线图） */
function ChartCard(props: {
    title: string;
    description: string;
    legend: string;
    color: string;
    points: LineChartPoint[];
    loading: boolean;
}) {
    const {title, description, legend, color, points, loading} = props;
    const {message} = App.useApp();

    const handleDownload = () => {
        if (!points.length) {
            message.warning('暂无可下载的数据');
            return;
        }
        const blob = new Blob([`\uFEFF${toCsv(points)}`], {type: 'text/csv;charset=utf-8'});
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${title}.csv`;
        link.click();
        URL.revokeObjectURL(url);
    };

    return (
        <div className={styles.chartCard}>
            <div className={styles.chartHead}>
                <div>
                    <div className={styles.chartTitle}>{title}</div>
                    <div className={styles.chartDesc}>{description}</div>
                </div>
                <div className={styles.chartActions}>
                    <span className={styles.chartLegend}>
                        <span className={styles.legendDot} style={{background: color}}/>
                        {legend}
                    </span>
                    <Tooltip title="下载数据">
                        <Button
                            type="text"
                            size="small"
                            icon={<DownloadOutlined/>}
                            aria-label="下载数据"
                            disabled={loading}
                            onClick={handleDownload}
                        />
                    </Tooltip>
                </div>
            </div>
            {loading ? (
                <div className={styles.chartLoading}>
                    <Skeleton active paragraph={{rows: 6}}/>
                </div>
            ) : (
                <LineChart data={points} color={color} height={280} unit="人"/>
            )}
        </div>
    );
}

export default function WelcomePage() {
    const {message} = App.useApp();
    const [loading, setLoading] = useState(true);
    const [statistics, setStatistics] = useState<DashboardStatistics>({});
    const [updatedAt, setUpdatedAt] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const result = await getDashboardStatistics();
            setStatistics(result.data || {});
            setUpdatedAt(formatTime(new Date()));
        } catch {
            message.error('统计数据加载失败');
        } finally {
            setLoading(false);
        }
    }, [message]);

    useEffect(() => {
        void load();
    }, [load]);

    const reservationPoints = toPoints(statistics.last30DaysReservationTrend);
    const userPoints = toPoints(statistics.last30DaysUserTrend);

    /** 取指标对应的趋势数组 */
    const trendOf = (trend?: TrendKey) => {
        if (trend === 'reservation') return statistics.last30DaysReservationTrend;
        if (trend === 'user') return statistics.last30DaysUserTrend;
        return undefined;
    };

    return (
        <PageContainer
            pageHeaderRender={false}
            token={{paddingInlinePageContainerContent: 0, paddingBlockPageContainerContent: 0}}
        >
            <div className={styles.page}>
                <div className={styles.hero}>
                    <div className={styles.heroCrumb}>
                        控制台&nbsp;›&nbsp;<b>信息统计</b>
                    </div>
                    <div className={styles.heroSub}>预约与会员运营核心指标一览</div>
                    <div className={styles.dateChip}>
                        <CalendarOutlined/>
                        {formatDate(new Date())} · {updatedAt ? `数据更新于 ${updatedAt}` : '数据加载中'}
                    </div>
                </div>

                <div className={styles.content}>
                    <Row gutter={[14, 14]}>
                        {STAT_ITEMS.map(item => {
                            const value = statistics[item.key] ?? 0;
                            const delta = item.cumulative
                                ? CUMULATIVE_DELTA
                                : buildDelta(value, trendValue(trendOf(item.trend), 1));
                            return (
                                <Col key={item.key} xs={12} sm={12} md={8} xl={4}>
                                    <StatCard
                                        label={item.label}
                                        value={value}
                                        icon={item.icon}
                                        color={item.color}
                                        background={item.background}
                                        delta={delta}
                                        loading={loading}
                                    />
                                </Col>
                            );
                        })}
                    </Row>

                    <div className={`${styles.sectionTitle} ${styles.sectionTitleGap}`}>数据统计</div>
                    <Row gutter={[14, 14]}>
                        <Col xs={24} lg={12}>
                            <ChartCard
                                title="近30天预约趋势"
                                description="每日预约人数变化"
                                legend="预约数"
                                color={RESERVATION_COLOR}
                                points={reservationPoints}
                                loading={loading}
                            />
                        </Col>
                        <Col xs={24} lg={12}>
                            <ChartCard
                                title="近30天用户增长趋势"
                                description="每日新增会员变化"
                                legend="用户"
                                color={USER_COLOR}
                                points={userPoints}
                                loading={loading}
                            />
                        </Col>
                    </Row>
                </div>
            </div>
        </PageContainer>
    );
}
