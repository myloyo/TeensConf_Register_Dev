import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, Button, Alert, Typography, Space, Steps, Result } from 'antd';
import { QrcodeOutlined, CheckCircleOutlined, LoadingOutlined, CloseCircleOutlined } from '@ant-design/icons';
import axios from '../services/api';
import { RegistrationResponse, PaymentConfirmationResponse, ApiError } from '../types';

const { Title, Paragraph } = Typography;
const { Step } = Steps;

const PaymentPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const registrationData = location.state as RegistrationResponse;
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [paymentStatus, setPaymentStatus] = useState<'pending' | 'success' | 'failed'>('pending');

  if (!registrationData) {
    navigate('/');
    return null;
  }

  const confirmPayment = async () => {
    setLoading(true);
    setError('');

    try {
      console.log('Confirming payment for:', registrationData.registrationId);
      await axios.post<PaymentConfirmationResponse>(
        `/api/registrations/${registrationData.registrationId}/confirm-payment`
      );
      
      setPaymentStatus('success');
      
      setTimeout(() => {
        navigate('/success');
      }, 2000);
      
    } catch (err) {
      console.error('Payment confirmation error:', err);
      setPaymentStatus('failed');
      if (axios.isAxiosError(err)) {
        const errorData = err.response?.data as ApiError;
        setError(errorData?.error || 'Оплата еще не поступила. Попробуйте позже.');
      } else {
        setError('Произошла неизвестная ошибка. Попробуйте позже.');
      }
    } finally {
      setLoading(false);
    }
  };

  const currentStep = paymentStatus === 'success' ? 2 : loading ? 1 : 0;

  if (paymentStatus === 'failed') {
    return (
      <div className="registration-container">
        <div className="header">
          <img src="/pics/photo.png" alt="Логотип Подростковой Конференции ТИНС" className="logo" />
        </div>

        <Card className="payment-container">
          <Result
            status="error"
            title="Оплата не подтверждена"
            subTitle={error || "Пожалуйста, попробуйте позже или обратитесь в поддержку"}
            extra={[
              <Button
                type="primary"
                key="retry"
                onClick={() => setPaymentStatus('pending')}
              >
                Попробовать снова
              </Button>,
              <Button
                key="home"
                onClick={() => navigate('/')}
              >
                На главную
              </Button>,
            ]}
          />
        </Card>
      </div>
    );
  }

  return (
    <div className="registration-container">
      <div className="header">
        <img src="/pics/photo.png" alt="Логотип Подростковой Конференции ТИНС" className="logo" />
      </div>

      <Card className="payment-container">
        <Steps current={currentStep} style={{ marginBottom: 40 }}>
          <Step title="Оплата" description="Оплатите регистрационный взнос" />
          <Step 
            title="Подтверждение" 
            description="Подтверждаем оплату" 
            icon={loading ? <LoadingOutlined /> : undefined}
          />
          <Step title="Завершено" description="Регистрация завершена" />
        </Steps>

        <div className="payment-header">
          <Title level={2}>Оплата регистрационного взноса</Title>
          <Paragraph>
            Сумма к оплате: <span className="amount">{registrationData.amount} руб</span>
          </Paragraph>
          <Paragraph type="secondary">
            ID регистрации: {registrationData.registrationId}
          </Paragraph>
        </div>

        {paymentStatus === 'success' && (
          <Alert
            message="Оплата подтверждена!"
            description="Перенаправляем на страницу успеха..."
            type="success"
            showIcon
            style={{ marginBottom: 24 }}
          />
        )}

        <div className="qr-section">
          <QrcodeOutlined style={{ fontSize: '48px', color: '#1890ff', marginBottom: '16px' }} />
          <br />
          <img 
            src={registrationData.qrCodeUrl} 
            alt="QR код для оплаты"
            className="qr-image"
          />
          <Paragraph>Отсканируйте QR-код для оплаты через СБП</Paragraph>
        </div>

        <div className="instructions">
          <Title level={4}>Инструкция по оплате:</Title>
          <ol>
            <li>Откройте приложение вашего банка</li>
            <li>Выберите оплату по QR-коду</li>
            <li>Наведите камеру на код выше</li>
            <li>Подтвердите платеж на сумму {registrationData.amount} руб</li>
            <li>После оплаты нажмите кнопку "Я оплатил(а)" ниже</li>
          </ol>
        </div>

        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Button
            type="primary"
            size="large"
            loading={loading}
            disabled={paymentStatus === 'success'}
            onClick={confirmPayment}
            icon={paymentStatus === 'success' ? <CheckCircleOutlined /> : undefined}
            style={{ height: '50px', fontSize: '18px', fontWeight: 600 }}
            block
          >
            {paymentStatus === 'success' ? 'Оплата подтверждена!' : 
             loading ? 'Проверка оплаты...' : 
             'Я оплатил(а)'}
          </Button>

          <div className="test-note">
            <Paragraph type="secondary">
              <strong>Примечание для тестирования:</strong> В тестовом режиме оплата эмулируется. 
              Просто нажмите кнопку "Я оплатил(а)" для продолжения.
            </Paragraph>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default PaymentPage;