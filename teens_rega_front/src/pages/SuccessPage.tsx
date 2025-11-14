import React from 'react';
import { Result, Button, Typography, Space } from 'antd';
import { SmileOutlined, HomeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Paragraph } = Typography;

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
        
        <Space direction="vertical" style={{ textAlign: 'center', marginTop: 40 }}>
          <Paragraph>
            Ваша регистрация успешно обработана и оплата подтверждена.
          </Paragraph>
          <Paragraph>
            Следите за новостями и дополнительной информацией о конференции в нашем <a href="https://t.me/confteens" target="_blank" rel="noopener noreferrer">тгк</a>
          </Paragraph>
          <Paragraph strong>
            Ждем вас на конференции!
          </Paragraph>
        </Space>
      </div>
    </div>
  );
};

export default SuccessPage;