import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Space } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import axios from '../services/api';

const { Title } = Typography;

const AdminLogin: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    setError('');
    
    try {
      const response = await axios.post('/api/admin/auth/login', values);
      localStorage.setItem('adminToken', response.data.token);
      navigate('/admin');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка авторизации');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '100vh',
    }}>
      <Card style={{ width: 400, boxShadow: '0 8px 24px rgba(0,0,0,0.1)' }}>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Title level={2} style={{ color: '#1890ff', margin: 0 }}>
              Админ Панель
            </Title>
            <p style={{ color: '#666', marginTop: 8 }}>
              Teens Conference Management
            </p>
          </div>
          
          {error && <Alert message={error} type="error" showIcon />}
          
          <Form
            name="adminLogin"
            onFinish={onFinish}
            autoComplete="off"
            size="large"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: 'Введите логин!' }]}
            >
              <Input 
                prefix={<UserOutlined />} 
                placeholder="Логин администратора" 
              />
            </Form.Item>
            
            <Form.Item
              name="password"
              rules={[{ required: true, message: 'Введите пароль!' }]}
            >
              <Input.Password 
                prefix={<LockOutlined />} 
                placeholder="Пароль" 
              />
            </Form.Item>
            
            <Form.Item>
              <Button 
                type="primary" 
                htmlType="submit" 
                loading={loading}
                style={{ width: '100%', height: 40 }}
              >
                Войти
              </Button>
            </Form.Item>
          </Form>
        </Space>
      </Card>
    </div>
  );
};

export default AdminLogin;