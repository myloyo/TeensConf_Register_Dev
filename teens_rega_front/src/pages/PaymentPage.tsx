import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { 
  Card, 
  Button, 
  Alert, 
  Typography, 
  Space, 
  Steps, 
  Result, 
  Upload, 
  message,
  Divider,
  List
} from 'antd';
import { 
  QrcodeOutlined, 
  CheckCircleOutlined, 
  LoadingOutlined,
  UploadOutlined,
  FilePdfOutlined,
  CloseCircleOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import axios from '../services/api';
import { isAxiosError } from 'axios';
import { RegistrationResponse, PaymentCompletionResponse, ApiError } from '../types';

const { Title, Paragraph, Text } = Typography;
const { Step } = Steps;

const PaymentPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const registrationData = location.state as RegistrationResponse;
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [detailedErrors, setDetailedErrors] = useState<string[]>([]);
  const [paymentStatus, setPaymentStatus] = useState<'pending' | 'success' | 'failed'>('pending');
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  if (!registrationData) {
    navigate('/');
    return null;
  }

  const sbpQrUrl = "https://qr.nspk.ru/AS1A003SCQ2PA3UQ9D4P7BDADQRQ9V7J?type=01&bank=100000000111&crc=29ED";


  const dummyRequest = async (options: any) => {
    const { onSuccess } = options;
    setTimeout(() => {
      onSuccess("ok");
    }, 0);
  };

  const handleUpload: UploadProps['onChange'] = (info) => {
    if (info.file.status === 'removed') {
      setFileList([]);
      return;
    }

    setFileList(info.fileList.slice(-1));
    
    setError('');
    setDetailedErrors([]);
  };

  const beforeUpload = (file: File) => {
    const isPdf = file.type === 'application/pdf';
    if (!isPdf) {
      message.error('Можно загружать только PDF файлы!');
      return false;
    }
    
    const isLt10M = file.size / 1024 / 1024 < 10;
    if (!isLt10M) {
      message.error('Файл должен быть меньше 10MB!');
      return false;
    }
    return false;
  };

  const parseDetailedErrors = (errorMessage: string): string[] => {
    if (errorMessage.includes(';')) {
      return errorMessage.split(';').map(err => err.trim());
    }
    return [errorMessage];
  };

  const completeRegistration = async () => {
    if (fileList.length === 0) {
      message.error('Пожалуйста, загрузите чек об оплате');
      return;
    }

    const file = fileList[0].originFileObj;
    if (!file) {
      message.error('Ошибка: файл не найден');
      return;
    }

    setLoading(true);
    setError('');
    setDetailedErrors([]);

    try {
      const formData = new FormData();
      formData.append('receiptFile', file);

      console.log('Completing registration for:', registrationData.registrationId);
      
      const response = await axios.post<PaymentCompletionResponse>(
        `/registrations/${registrationData.registrationId}/complete`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );
      
      if (response.data.success) {
        setPaymentStatus('success');
        message.success(response.data.message);
        
        setTimeout(() => {
          navigate('/success');
        }, 3000);
      } else {
        setPaymentStatus('failed');
        setError(response.data.error || 'Ошибка при завершении регистрации');
      }
      
    } catch (err: unknown) {
      console.error('Registration completion error:', err);
      setPaymentStatus('failed');

      if (isAxiosError(err)) {
        const errorData = err.response?.data as ApiError;
        const errorMessage = errorData?.error || 'Произошла ошибка при завершении регистрации. Попробуйте позже.';
        setError(errorMessage);
        setDetailedErrors(parseDetailedErrors(errorMessage));
      } else if (err instanceof Error) {
        setError(err.message);
        setDetailedErrors(parseDetailedErrors(err.message));
      } else {
        setError('Произошла неизвестная ошибка. Попробуйте позже.');
      }
    } finally {
      setLoading(false);
    }
  };

  const removeFile = () => {
    setFileList([]);
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
            title="Регистрация не завершена"
            subTitle="Пожалуйста, исправьте следующие ошибки и попробуйте снова:"
            extra={[
              <Button
                type="primary"
                key="retry"
                onClick={() => {
                  setPaymentStatus('pending');
                  setFileList([]);
                  setError('');
                  setDetailedErrors([]);
                }}
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
          
          {detailedErrors.length > 0 && (
            <div style={{ marginTop: 24, textAlign: 'left' }}>
              <Alert
                message="Обнаруженные проблемы в чеке:"
                type="error"
                showIcon
                description={
                  <List
                    size="small"
                    dataSource={detailedErrors}
                    renderItem={(error, index) => (
                      <List.Item>
                        <Space>
                          <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                          <Text>{error}</Text>
                        </Space>
                      </List.Item>
                    )}
                  />
                }
              />
              
              <div style={{ marginTop: 16, padding: 16, background: '#fff7e6', borderRadius: 6 }}>
                <Title level={5}>Как исправить:</Title>
                <ul>
                  <li>Убедитесь, что оплата сделана на правильные реквизиты</li>
                  <li>Проверьте, что сумма составляет ровно 500 рублей</li>
                  <li>Убедитесь, что в чеке указан ИНН 6453041398</li>
                  <li>Проверьте, что получатель - Церковь "Слово Жизни" Саратов</li>
                  <li>Если проблемы остаются, обратитесь к служителям в telegram: @plashbik или @myloyorrr_still</li>
                </ul>
              </div>
            </div>
          )}
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
          <Step title="Перевод" description="Внесите добровольное пожертвование" />
          <Step 
            title="Подтверждение" 
            description="Загрузите чек и завершите регистрацию" 
            icon={loading ? <LoadingOutlined /> : undefined}
          />
          <Step title="Завершено" description="Регистрация завершена" />
        </Steps>

        <div className="payment-header">
          <Title level={2}>Добровольное пожертвование</Title>
          <Paragraph>
            Сумма пожертвования: <span className="amount">500 руб</span>
          </Paragraph>
        </div>

        {paymentStatus === 'success' && (
          <Alert
            message="Регистрация завершена успешно!"
            description="Проверьте вашу почту для получения подтверждения. Перенаправляем на страницу успеха..."
            type="success"
            showIcon
            style={{ marginBottom: 24 }}
          />
        )}

        <div className="qr-section">
          <Title level={4}>Перевод через СБП</Title>
          <a href={sbpQrUrl} target="_blank" rel="noopener noreferrer">
            <img 
              src={`https://api.qrserver.com/v1/create-qr-code/?size=256x256&data=${encodeURIComponent(sbpQrUrl)}`}
              alt="QR код для оплаты через СБП"
              className="qr-image"
              style={{ border: '1px solid #e8e8e8', borderRadius: '8px' }}
            />
          </a>
          <Paragraph>
            Отсканируйте QR-код для перевода через СБП или{' '}
            <a href={sbpQrUrl} target="_blank" rel="noopener noreferrer">
              нажмите здесь для открытия в приложении банка
            </a>
          </Paragraph>
        </div>

        <Divider />

        <div className="upload-section">
          <Title level={4}>Подтверждение оплаты</Title>
          <Paragraph>
            После оплаты загрузите чек (PDF файл) для завершения регистрации
          </Paragraph>
          
          <Upload
            accept=".pdf"
            fileList={fileList}
            onChange={handleUpload}
            beforeUpload={beforeUpload}
            customRequest={dummyRequest}
            maxCount={1}
            listType="text"
            onRemove={removeFile}
          >
            <Button 
              icon={<UploadOutlined />} 
              size="large"
              disabled={fileList.length > 0}
            >
              Выберите файл чека (PDF)
            </Button>
          </Upload>

          <div style={{ marginTop: 16 }}>
            <Text type="secondary">
              <FilePdfOutlined /> Поддерживаются только PDF файлы. Максимальный размер: 10MB
            </Text>
          </div>

          {fileList.length > 0 && (
            <div style={{ marginTop: 16 }}>
              <Alert
                message={`Выбран файл: ${fileList[0].name}`}
                type="info"
                action={
                  <Button size="small" type="text" onClick={removeFile}>
                    <DeleteOutlined />
                  </Button>
                }
              />
            </div>
          )}
        </div>

        <Divider />

        <div className="instructions">
          <Title level={4}>Требования к чеку:</Title>
          <Alert
            message="Для успешной проверки в чеке должны быть:"
            description={
              <ul>
                <li><strong>Получатель:</strong> ЦЕРКОВЬ СЛОВО ЖИЗНИ_SBP или Церковь "Слово Жизни" Саратов</li>
                <li><strong>ИНН:</strong> 6453041398</li>
                <li><strong>Банк получателя:</strong> ПАО СБЕРБАНК</li>
                <li><strong>Сумма:</strong> ровно 500 рублей</li>
              </ul>
            }
            type="info"
            showIcon
          />
        </div>

        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Button
            type="primary"
            size="large"
            loading={loading}
            disabled={paymentStatus === 'success' || fileList.length === 0}
            onClick={completeRegistration}
            icon={paymentStatus === 'success' ? <CheckCircleOutlined /> : undefined}
            style={{ minHeight: '44px', fontSize: '16px', fontWeight: 600 }}
            block
          >
            {paymentStatus === 'success' ? 'Регистрация завершена!' : 
             loading ? 'Проверка чека и завершение регистрации...' : 
             'Завершить регистрацию'}
          </Button>

          <Alert
            message="Важная информация"
            description="После проверки чека на вашу почту будет отправлено письмо с QR-кодом для подтверждения регистрации на стойке. Сохраните это письмо!"
            type="info"
            showIcon
          />
        </Space>
      </Card>
    </div>
  );
};

export default PaymentPage;