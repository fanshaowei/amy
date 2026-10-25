import {CameraOutlined, LoadingOutlined, UserOutlined} from '@ant-design/icons';
import {App, Avatar, Typography, Upload} from 'antd';
import ImgCrop from 'antd-img-crop';
// antd-img-crop 自身样式是自注入的，但它底层依赖的 react-easy-crop 样式需要显式引入，
// 否则裁剪弹层里图片不会按 reactEasyCrop_* 类名定位，画面会错位。
import 'react-easy-crop/react-easy-crop.css';
import React, {useState} from 'react';
import {uploadUserAvatar} from '@/services/system/user';
import styles from './index.less';

/** 后端 MimeTypeUtils.IMAGE_EXTENSION = {bmp, gif, jpg, jpeg, png}，这里与之保持一致 */
const ACCEPT = 'image/bmp,image/gif,image/jpg,image/jpeg,image/png';
const MAX_SIZE_MB = 5;

interface AvatarUploadProps {
    /** 当前头像 URL */
    value?: string;
    /** 头像 URL 变化回调（上传成功后回填） */
    onChange?: (url: string) => void;
}

/**
 * 头像上传（对齐 ruoyi-ui 个人中心：点击头像 → 上传前裁剪 → 上传 → 回填 URL）。
 *
 * 关键点：antd-img-crop 的 ImgCrop 会替换内部 Upload 的 beforeUpload ——
 * 先弹裁剪框，用户点「确定」后，才把裁剪后的 file 交给我们的 beforeUpload。
 * 所以我们只需在自己的 beforeUpload 里手动上传并 return false
 * （false 会阻止 antd Upload 再按 action 自动发一次请求）。
 */
const AvatarUpload: React.FC<AvatarUploadProps> = ({value, onChange}) => {
    const {message} = App.useApp();
    const [uploading, setUploading] = useState(false);

    const beforeUpload = async (file: File) => {
        if (file.size / 1024 / 1024 > MAX_SIZE_MB) {
            message.error(`头像大小不能超过 ${MAX_SIZE_MB}MB`);
            return false;
        }
        setUploading(true);
        try {
            const result = await uploadUserAvatar(file);
            onChange?.(result.imgUrl);
            message.success('头像更新成功');
        } finally {
            setUploading(false);
        }
        return false;
    };

    return (
        <div className={styles.avatarWrapper}>
            <ImgCrop
                aspect={1}
                cropShape="round"
                showGrid
                showReset
                zoomSlider
                rotationSlider
                quality={1}
                modalTitle="修改头像"
                modalOk="确定"
                modalCancel="取消"
                resetText="重置"
            >
                <Upload
                    accept={ACCEPT}
                    showUploadList={false}
                    maxCount={1}
                    beforeUpload={beforeUpload}
                >
                    <div className={styles.avatarBox}>
                        <Avatar size={104} src={value} icon={<UserOutlined/>} alt="头像"/>
                        <span className={styles.avatarMask}>
                            {uploading ? <LoadingOutlined/> : <CameraOutlined/>}
                            <em>更换头像</em>
                        </span>
                    </div>
                </Upload>
            </ImgCrop>
            <Typography.Text type="secondary" style={{fontSize: 12}}>
                支持 JPG / PNG / BMP / GIF，不超过 {MAX_SIZE_MB}MB
            </Typography.Text>
        </div>
    );
};

export default AvatarUpload;
