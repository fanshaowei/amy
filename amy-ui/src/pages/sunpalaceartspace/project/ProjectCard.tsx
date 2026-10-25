import {
    ClockCircleOutlined,
    CopyOutlined,
    DeleteOutlined,
    EditOutlined,
    FieldTimeOutlined,
    HistoryOutlined,
    PictureOutlined,
    TeamOutlined
} from '@ant-design/icons';
import {Card, Checkbox, Image, Space, Tag, Tooltip, Typography} from 'antd';
import type {ReactNode} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import type {ProjectRecord} from '@/services/sunpalaceartspace/project';
import styles from '../spas.less';

/** 封面区高度，与 Image 的 height 保持一致 */
const COVER_HEIGHT = 168;

/** 去掉字符串里可能存在的日期前缀，只显示时间（用于卡片展示） */
const timeOnly = (value?: string | null) => (value || '').replace(/^\d{4}-\d{2}-\d{2}[T\s]+/, '');

/** 卡片中的「标签 / 数值」小块 */
function MetaItem({icon, label, children}: { icon: ReactNode; label: string; children?: ReactNode }) {
    return (
        <div className={styles.metaItem}>
            <span className={styles.metaLabel}>{icon}{label}</span>
            <span className={styles.metaValue}>{children ?? '-'}</span>
        </div>
    );
}

export interface ProjectCardProps {
    project: ProjectRecord;
    /** 是否已被批量选中 */
    selected: boolean;
    onSelectChange: (checked: boolean) => void;
    onEdit: () => void;
    onCopy: () => void;
    onDelete: () => void;
}

/** 项目管理卡片：封面 + 标题 + 规则摘要 + 操作区 */
export function ProjectCard({project, selected, onSelectChange, onEdit, onCopy, onDelete}: ProjectCardProps) {
    const visible = project.status === '0';

    return (
        <Card
            className={`${styles.projectCard} ${selected ? styles.projectCardActive : ''}`}
            styles={{body: {padding: 0}}}
        >
            <div className={styles.projectCover}>
                {project.coverArtUrl
                    ? <Image src={project.coverArtUrl} preview={false} width="100%" height={COVER_HEIGHT}
                             alt={`${project.projectName}封面`}/>
                    : <div className={styles.projectCoverEmpty}>
                        <PictureOutlined style={{fontSize: 22}}/>
                        <span>暂无封面</span>
                    </div>}
                <div className={styles.projectCoverShade}/>
                <div className={`${styles.projectCorner} ${styles.projectCornerLeft}`}>
                    <Checkbox
                        className={styles.checkPill}
                        checked={selected}
                        aria-label={`选择项目 ${project.projectName}`}
                        onChange={e => onSelectChange(e.target.checked)}
                    />
                </div>
                <div className={`${styles.projectCorner} ${styles.projectCornerRight}`}>
                    <Tag bordered={false} color={visible ? 'success' : 'default'}
                         style={{margin: 0, borderRadius: 20, fontWeight: 600, boxShadow: '0 2px 8px rgba(28,35,51,0.12)'}}>
                        {visible ? '展示中' : '已隐藏'}
                    </Tag>
                </div>
            </div>

            <div className={styles.projectBody}>
                <Typography.Title level={5} className={styles.projectTitle} ellipsis={{rows: 1}}>
                    {project.projectName}
                </Typography.Title>

                <div className={styles.projectMeta}>
                    <MetaItem icon={<ClockCircleOutlined/>} label="可预约时段">
                        {`${timeOnly(project.reservationStartTime) || '--'} - ${timeOnly(project.reservationEndTime) || '--'}`}
                    </MetaItem>
                    <MetaItem icon={<HistoryOutlined/>} label="预约规则">
                        {[
                            project.reservationIntervalSecond != null && `每 ${project.reservationIntervalSecond} 分钟`,
                            project.advanceReservationDays != null && `提前 ${project.advanceReservationDays} 天`
                        ].filter(Boolean).join(' · ') || '-'}
                    </MetaItem>
                    <MetaItem icon={<TeamOutlined/>} label="人数规则">
                        {[
                            project.reservationCount != null && `每场 ${project.reservationCount} 人`,
                            project.travelerCount != null && `随行 ${project.travelerCount} 人`
                        ].filter(Boolean).join(' · ') || '-'}
                    </MetaItem>
                    <MetaItem icon={<FieldTimeOutlined/>} label="当日截止">
                        {timeOnly(project.cutOffTime) || '-'}
                    </MetaItem>
                </div>

                <div className={styles.projectFooter}>
                    <span className={styles.footerMeta}>
                        {project.createTime ? `发布于 ${project.createTime.slice(0, 10)}` : '暂无发布时间'}
                    </span>
                    <Space className={styles.footerActions} size={0}>
                        <Tooltip title="编辑项目">
                            <PermissionButton type="text" size="small" icon={<EditOutlined/>} aria-label="编辑项目"
                                              permission="sunpalaceartspace:project:edit" onClick={onEdit}/>
                        </Tooltip>
                        <Tooltip title="复制页面地址">
                            <PermissionButton type="text" size="small" icon={<CopyOutlined/>} aria-label="复制页面地址"
                                              permission="sunpalaceartspace:project:edit" onClick={onCopy}/>
                        </Tooltip>
                        <Tooltip title="删除项目">
                            <PermissionButton type="text" size="small" danger icon={<DeleteOutlined/>}
                                              aria-label="删除项目"
                                              permission="sunpalaceartspace:project:remove" onClick={onDelete}/>
                        </Tooltip>
                    </Space>
                </div>
            </div>
        </Card>
    );
}

export default ProjectCard;
