import {DownloadOutlined, ReloadOutlined} from '@ant-design/icons';
import {PageContainer} from '@ant-design/pro-components';
import {App, Skeleton, Tooltip} from 'antd';
import {useCallback, useEffect, useState} from 'react';
import LineChart from '@/components/LineChart';
import type {LineChartPoint} from '@/components/LineChart';
import {getDashboardStatistics} from '@/services/sunpalaceartspace/dashboard';
import type {DashboardStatistics, DashboardTrendItem} from '@/services/sunpalaceartspace/dashboard';

/** 可展示的数值型统计字段 */
type StatKey =
    | 'todayReservationCount'
    | 'todayVerifyCount'
    | 'tomorrowReservationCount'
    | 'totalReservationCount'
    | 'todayNewUserCount'
    | 'totalUserCount';

const STAT_ITEMS: Array<{ key: StatKey; label: string }> = [
    {key: 'todayReservationCount', label: '今日预约人数'},
    {key: 'todayVerifyCount', label: '今日核销人数'},
    {key: 'tomorrowReservationCount', label: '明日预约人数'},
    {key: 'totalReservationCount', label: '预约总数'},
    {key: 'todayNewUserCount', label: '今日新增会员'},
    {key: 'totalUserCount', label: '会员总数'}
];

const RESERVATION_COLOR = '#4096ff';
const USER_COLOR = '#4096ff';

/** 后端趋势数据 -> 图表数据（日期裁剪为 MM/DD） */
const toPoints = (list?: DashboardTrendItem[]): LineChartPoint[] =>
    (list || []).map(item => ({
        label: item.date ? item.date.slice(5).replace('-', '/') : '',
        title: item.date,
        value: item.count ?? 0
    }));

/** 导出趋势数据为 CSV */
const downloadTrend = (name: string, points: LineChartPoint[]) => {
    const csv = ['日期,数量', ...points.map(p => `${p.title || p.label},${p.value}`)].join('\n');
    const blob = new Blob([`\uFEFF${csv}`], {type: 'text/csv;charset=utf-8'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${name}.csv`;
    link.click();
    URL.revokeObjectURL(url);
};

/** 带左侧色条的卡片 */
function SectionCard({title, children}: { title: string; children: React.ReactNode }) {
    return (
        <div style={{
            background: '#fff',
            borderRadius: 8,
            marginBottom: 16,
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.04)'
        }}>
            <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '12px 20px',
                borderBottom: '1px solid #f0f0f0'
            }}>
                <span style={{width: 3, height: 14, borderRadius: 2, background: '#00b96b'}}/>
                <span style={{fontSize: 15, fontWeight: 600}}>{title}</span>
            </div>
            <div style={{padding: '24px 20px'}}>{children}</div>
        </div>
    );
}

/** 单个趋势图表（标题 + 图例 + 下载 + 折线图） */
function TrendPanel(props: {
    title: string;
    legend: string;
    color: string;
    points: LineChartPoint[];
}) {
    const {title, legend, color, points} = props;
    return (
        <div style={{flex: '1 1 460px', minWidth: 320}}>
            <div style={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                padding: '0 8px'
            }}>
                <div>
                    <div style={{fontSize: 15, fontWeight: 600}}>{title}</div>
                    <div style={{fontSize: 12, color: '#8c8c8c', marginTop: 4}}>最近30天</div>
                </div>
                <div style={{display: 'flex', alignItems: 'center', gap: 16, paddingTop: 4}}>
                    <span style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        fontSize: 13,
                        color: '#595959'
                    }}>
                        <span style={{
                            width: 8,
                            height: 8,
                            borderRadius: '50%',
                            border: `2px solid ${color}`,
                            boxSizing: 'content-box'
                        }}/>
                        {legend}
                    </span>
                    <Tooltip title="下载数据">
                        <DownloadOutlined
                            style={{color: '#8c8c8c', cursor: 'pointer'}}
                            onClick={() => downloadTrend(title, points)}
                        />
                    </Tooltip>
                </div>
            </div>
            <LineChart data={points} color={color} height={300}/>
        </div>
    );
}

export default function WelcomePage() {
    const {message} = App.useApp();
    const [loading, setLoading] = useState(true);
    const [statistics, setStatistics] = useState<DashboardStatistics>({});

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const result = await getDashboardStatistics();
            setStatistics(result.data || {});
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

    return (
        <PageContainer header={{title: false, breadcrumb: {items: [{title: '控制台'}]}}}>
            <SectionCard title="信息统计">
                {loading ? <Skeleton active paragraph={{rows: 2}}/> : (
                    <div style={{display: 'flex', flexWrap: 'wrap', rowGap: 24}}>
                        {STAT_ITEMS.map(item => (
                            <div key={item.key} style={{flex: '1 1 0', minWidth: 140, textAlign: 'center'}}>
                                <div style={{fontSize: 28, fontWeight: 600, lineHeight: 1.3, color: '#1f1f1f'}}>
                                    {statistics[item.key] ?? 0}
                                </div>
                                <div style={{
                                    marginTop: 8,
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: 4,
                                    fontSize: 13,
                                    color: '#595959'
                                }}>
                                    <ReloadOutlined style={{fontSize: 12, color: '#8c8c8c'}}/>
                                    {item.label}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </SectionCard>

            <SectionCard title="数据统计">
                {loading ? <Skeleton active paragraph={{rows: 6}}/> : (
                    <div style={{display: 'flex', flexWrap: 'wrap', gap: 24}}>
                        <TrendPanel title="近30天预约趋势" legend="预约数" color={RESERVATION_COLOR}
                                    points={reservationPoints}/>
                        <TrendPanel title="近30天用户增长趋势" legend="用户" color={USER_COLOR} points={userPoints}/>
                    </div>
                )}
            </SectionCard>
        </PageContainer>
    );
}
