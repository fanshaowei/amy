import { LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { LoginFormPage, ProFormCaptcha, ProFormCheckbox, ProFormText } from '@ant-design/pro-components';
import { history, useModel, useSearchParams } from '@umijs/max';
import { App, Image, theme } from 'antd';
import Cookies from 'js-cookie';
import { useEffect, useState } from 'react';
import { getCaptcha, getRouters, getUserInfo, login } from '@/services/auth';
import type { LoginParams, LoginResult, BackendRoute } from '@/types/api';
import { setExpiresIn, setToken } from '@/utils/auth';
import { decrypt, encrypt } from '@/utils/encryption';
import background from '@/assets/login-background.jpg';
import styles from './index.less';

interface LoginFormValues extends LoginParams {
  rememberMe?: boolean;
}

const USERNAME_KEY = 'username';
const PASSWORD_KEY = 'password';
const REMEMBER_KEY = 'rememberMe';

export default function LoginPage() {
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const { setInitialState } = useModel('@@initialState');
  const [searchParams] = useSearchParams();
  const [captchaEnabled, setCaptchaEnabled] = useState(true);
  const [captchaUrl, setCaptchaUrl] = useState('');
  const [uuid, setUuid] = useState('');

  const loadCaptcha = async () => {
    try {
      const result = await getCaptcha();
      const enabled = result.captchaEnabled ?? true;
      setCaptchaEnabled(enabled);
      setUuid(result.uuid || '');
      setCaptchaUrl(enabled && result.img ? `data:image/gif;base64,${result.img}` : '');
    } catch {
      setCaptchaUrl('');
      message.warning('验证码加载失败，请确认后端服务已启动');
    }
  };

  useEffect(() => {
    void loadCaptcha();
  }, []);

  const remembered = Cookies.get(REMEMBER_KEY) === 'true';
  const initialValues: LoginFormValues = {
    username: Cookies.get(USERNAME_KEY) || 'admin',
    password: remembered ? decrypt(Cookies.get(PASSWORD_KEY) || '') : 'admin123',
    rememberMe: remembered
  };

  const handleSubmit = async (values: LoginFormValues) => {
    try {
      const result = await login({ ...values, uuid });
      const loginData = result.data as LoginResult | undefined;
      const accessToken = loginData?.access_token;
      const expiresIn = loginData?.expires_in;

      if (!accessToken) {
        throw new Error('登录返回令牌为空');
      }

      setToken(accessToken);
      if (typeof expiresIn === 'number') {
        setExpiresIn(expiresIn);
      }

      if (values.rememberMe) {
        Cookies.set(USERNAME_KEY, values.username, { expires: 30 });
        Cookies.set(PASSWORD_KEY, encrypt(values.password), { expires: 30 });
        Cookies.set(REMEMBER_KEY, 'true', { expires: 30 });
      } else {
        Cookies.remove(USERNAME_KEY);
        Cookies.remove(PASSWORD_KEY);
        Cookies.remove(REMEMBER_KEY);
      }

      const [userInfoResult, routeResult] = await Promise.all([getUserInfo(), getRouters()]);
      const routes = routeResult.data as BackendRoute[];
      await setInitialState({
        currentUser: userInfoResult.user,
        roles: userInfoResult.roles || [],
        permissions: userInfoResult.permissions || [],
        routes: routes || []
      });
      message.success('登录成功');
      history.push(searchParams.get('redirect') || '/');
      return true;
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败，请稍后重试');
      if (captchaEnabled) await loadCaptcha();
      return false;
    }
  };

  return (
    <div className={styles.page} style={{ backgroundImage: `url(${background})` }}>
      <LoginFormPage<LoginFormValues>
        title="若依管理系统"
        subTitle="Ant Design Pro 企业级管理平台"
        initialValues={initialValues}
        onFinish={handleSubmit}
        backgroundImageUrl={background}
        containerStyle={{ backgroundColor: token.colorBgContainer, backdropFilter: 'blur(8px)' }}
        submitter={{ searchConfig: { submitText: '登录' }, submitButtonProps: { block: true, size: 'large' } }}
      >
        <ProFormText
          name="username"
          fieldProps={{ size: 'large', prefix: <UserOutlined /> }}
          placeholder="请输入账号"
          rules={[{ required: true, message: '请输入您的账号' }]}
        />
        <ProFormText.Password
          name="password"
          fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
          placeholder="请输入密码"
          rules={[{ required: true, message: '请输入您的密码' }]}
        />
        {captchaEnabled && (
          <div className={styles.captchaRow}>
            <ProFormCaptcha
              name="code"
              fieldProps={{ size: 'large', prefix: <SafetyCertificateOutlined /> }}
              placeholder="请输入验证码"
              rules={[{ required: true, message: '请输入验证码' }]}
              captchaProps={{ style: { display: 'none' } }}
              onGetCaptcha={async () => undefined}
            />
            <button type="button" className={styles.captchaButton} onClick={() => void loadCaptcha()} aria-label="刷新验证码">
              {captchaUrl ? <Image preview={false} src={captchaUrl} alt="验证码" height={40} /> : '点击刷新'}
            </button>
          </div>
        )}
        <ProFormCheckbox name="rememberMe">记住密码</ProFormCheckbox>
      </LoginFormPage>
      <footer className={styles.footer}>Copyright © 2018-2026 RuoYi. All Rights Reserved.</footer>
    </div>
  );
}
