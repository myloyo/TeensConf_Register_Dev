import React from 'react';
import { Result, Button, Typography, Space, Card } from 'antd';
import { SmileOutlined, HomeOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const {Paragraph, Title } = Typography;

const SuccessPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="registration-container">
      <div className="header">
        <img src="/pics/photo.png" alt="Логотип Подростковой Конференции ТИНС" className="logo" />
      </div>

      <div className="success-container">
        <Result
          icon={<SmileOutlined />}
          status="success"
          title="Регистрация успешно завершена!"
          subTitle="Спасибо за регистрацию на подростковую конференцию ТИНС!"
          extra={[
            <Button
              type="primary"
              key="home"
              size="large"
              icon={<HomeOutlined />}
              onClick={() => navigate('/')}
            >
              На главную
            </Button>,
            <Button
              key="another"
              size="large"
              onClick={() => navigate('/')}
            >
              Зарегистрировать еще одного
            </Button>,
          ]}
        />
        
        <Card style={{ marginTop: 24, textAlign: 'left' }}>
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Title level={4}>Что дальше?</Title>
            <Paragraph>
              <MailOutlined /> <strong>Проверьте вашу почту</strong> - мы отправили письмо с QR-кодом для подтверждения регистрации на стойке.
            </Paragraph>
            <Paragraph>
              <strong>Сохраните письмо</strong> - покажите QR-код из письма при регистрации на конференции.
            </Paragraph>
          </Space>
        </Card>

        <Space direction="vertical" style={{ textAlign: 'center', marginTop: 40, width: '100%' }}>
          <Paragraph>
            Следите за конференцией в нашем{' '}
            <a href="https://t.me/confteens" target="_blank" rel="noopener noreferrer">
              Telegram-канале
            </a>
          </Paragraph>
          <Paragraph strong style={{ fontSize: '16px' }}>
            Ждем тебя на конференции!
          </Paragraph>
        </Space>
      </div>
    </div>
  );
};

export default SuccessPage;