import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Card, 
  Statistic, 
  Row, 
  Col, 
  Button, 
  Space,
  Typography,
  Alert,
  Tag
} from 'antd';
import { 
  DownloadOutlined, 
  UserOutlined, 
  CheckCircleOutlined,
  ClockCircleOutlined 
} from '@ant-design/icons';
import axios from '../services/api';

const { Title } = Typography;

interface Registration {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  city: string;
  role: string;
  needAccommodation: boolean;
  registrationCompletedAt: string | null;
  paymentReceipt: {
    id: number;
    fileName: string;
  } | null;
}

interface Stats {
  totalRegistrations: number;
  completedRegistrations: number;
  pendingRegistrations: number;
}

const AdminPage: React.FC = () => {
  const [registrations, setRegistrations] = useState<Registration[]>([]);
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [registrationsRes, statsRes] = await Promise.all([
        axios.get('/api/admin/registrations'),
        axios.get('/api/admin/stats')
      ]);
      
      setRegistrations(registrationsRes.data.content || registrationsRes.data);
      setStats(statsRes.data);
    } catch (err) {
      setError('Ошибка загрузки данных');
      console.error('Admin data fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  const downloadReceipt = async (registrationId: number, fileName: string) => {
    try {
      const response = await axios.get(`/api/admin/registrations/${registrationId}/receipt`, {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download error:', err);
      alert('Ошибка при скачивании чека');
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: 'Имя',
      dataIndex: 'firstName',
      key: 'firstName',
    },
    {
      title: 'Фамилия',
      dataIndex: 'lastName',
      key: 'lastName',
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Телефон',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: 'Город',
      dataIndex: 'city',
      key: 'city',
    },
    {
      title: 'Роль',
      dataIndex: 'role',
      key: 'role',
    },
    {
      title: 'Расселение',
      dataIndex: 'needAccommodation',
      key: 'needAccommodation',
      render: (need: boolean) => need ? 'Да' : 'Нет',
    },
    {
      title: 'Статус',
      key: 'status',
      render: (record: Registration) => (
        <Tag color={record.registrationCompletedAt ? 'green' : 'orange'}>
          {record.registrationCompletedAt ? 'Завершено' : 'В процессе'}
        </Tag>
      ),
    },
    {
      title: 'Действия',
      key: 'actions',
      render: (record: Registration) => (
        <Space>
          {record.paymentReceipt && (
            <Button
              icon={<DownloadOutlined />}
              size="small"
              onClick={() => downloadReceipt(record.id, record.paymentReceipt!.fileName)}
            >
              Чек
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Title level={2}>Панель администратора</Title>
      
      {error && (
        <Alert message={error} type="error" style={{ marginBottom: 16 }} />
      )}

      {/* Статистика */}
      {stats && (
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={8}>
            <Card>
              <Statistic
                title="Всего регистраций"
                value={stats.totalRegistrations}
                prefix={<UserOutlined />}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="Завершенные"
                value={stats.completedRegistrations}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="В процессе"
                value={stats.pendingRegistrations}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ color: '#cf1322' }}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* Таблица регистраций */}
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={4} style={{ margin: 0 }}>Список регистраций</Title>
          <Button onClick={fetchData} loading={loading}>
            Обновить
          </Button>
        </div>
        
        <Table
          columns={columns}
          dataSource={registrations}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 1000 }}
        />
      </Card>
    </div>
  );
};

export default AdminPage;