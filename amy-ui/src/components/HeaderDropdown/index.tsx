import {Dropdown} from 'antd';
import type {DropdownProps} from 'antd';
import React from 'react';

/**
 * 对标 Ant Design Pro 官方的 HeaderDropdown 组件：
 * 只是 antd Dropdown 的薄包装，用于给右上角头像下拉统一挂载 overlayClassName，
 * 便于定制弹层样式（官方 aimed at 统一收拢头部弹层的 z-index / 圆角 / 阴影）。
 */
const HeaderDropdown: React.FC<DropdownProps> = ({overlayClassName, ...restProps}) => (
    <Dropdown overlayClassName={overlayClassName} {...restProps} />
);

export default HeaderDropdown;
