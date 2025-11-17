import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Form,
  Input,
  Button,
  Card,
  Radio,
  Checkbox,
  Alert,
  Space,
  Typography,
} from 'antd';
import { UserOutlined, PhoneOutlined, MessageOutlined, HomeOutlined, MailOutlined } from '@ant-design/icons';
import axios from '../services/api';
import { RegistrationRequest, RegistrationResponse, ApiError } from '../types';
import { isAxiosError } from 'axios';

const { Title, Paragraph } = Typography;

const RegistrationPage: React.FC = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [birthDate, setBirthDate] = useState<string>('');

  const calculateAge = (birthDate: string): number => {
    if (!birthDate) return 0;
    const [day, month, year] = birthDate.split('/');
    const birth = new Date(Number(year), Number(month) - 1, Number(day));
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  const age = calculateAge(birthDate);
  const isUnder18 = age < 18;
  const isUnder14 = age < 14;

  const onFinish = async (values: RegistrationRequest) => {
    setLoading(true);
    setError('');

    try {
      console.log('Registration data:', values);
      const response = await axios.post<RegistrationResponse>('/api/registrations', values);
      console.log('Registration successful:', response.data);
      
      // Переходим на страницу оплаты с данными
      navigate('/payment', { state: response.data });
    } catch (err) {
      console.error('Registration error:', err);
      if (isAxiosError(err)) {
        const errorData = err.response?.data as ApiError;
        setError(errorData?.error || 'Произошла ошибка при регистрации. Попробуйте еще раз.');
      } else {
        setError('Произошла неизвестная ошибка. Попробуйте еще раз.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="registration-container">
      <div className="header">
        <img src="/pics/photo.png" alt="Логотип Подростковой Конференции ТИНС" className="logo" />
      </div>

      <Card className="registration-form">
        {error && (
          <Alert
            message="Ошибка регистрации"
            description={error}
            type="error"
            showIcon
            closable
            style={{ marginBottom: 24 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          {/* Регистрация */}
          <div className="form-section">
            <Title level={3}>Регистрация</Title>
            
            <div className="form-row">
              <div className="form-group">
                <Form.Item
                  label="Имя"
                  name="firstName"
                  rules={[
                    { required: true, message: 'Пожалуйста, введите ваше имя' },
                    { min: 2, message: 'Имя должно содержать минимум 2 символа' },
                  ]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="Введите ваше имя"
                  />
                </Form.Item>
              </div>
              
              <div className="form-group">
                <Form.Item
                  label="Фамилия"
                  name="lastName"
                  rules={[
                    { required: true, message: 'Пожалуйста, введите вашу фамилию' },
                    { min: 2, message: 'Фамилия должна содержать минимум 2 символа' },
                  ]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="Введите вашу фамилию"
                  />
                </Form.Item>
              </div>
            </div>

            <Form.Item
              label="Дата рождения"
              name="birthDate"
              rules={[
                { required: true, message: 'Пожалуйста, введите дату рождения' },
                { pattern: /^\d{2}\/\d{2}\/\d{4}$/, message: 'Формат: дд/мм/гггг' },
              ]}
            >
              <Input
                placeholder="дд/мм/гггг (например: 15/05/2005)"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setBirthDate(e.target.value)}
              />
            </Form.Item>

            {birthDate && (
              <Alert
                message={`Возраст: ${age} ${isUnder18 ? ' (Несовершеннолетний)' : ''}${isUnder14 ? ' (Младше 14 лет)' : ''}`}
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: 'Пожалуйста, введите электронную почту' },
                { 
                  type: 'email', 
                  message: 'Введите корректный email адрес' 
                }
              ]}
            >
              <Input
                prefix={<MailOutlined />}
                placeholder="example@mail.ru"
              />
            </Form.Item>

            <Form.Item
              label="Телефон для связи"
              name="phone"
              rules={[
                { required: true, message: 'Пожалуйста, введите телефон' },
                { pattern: /^\+7\d{10}$/, message: 'Формат: +7XXXXXXXXXX' },
              ]}
            >
              <Input
                prefix={<PhoneOutlined />}
                placeholder="+79161234567"
              />
            </Form.Item>

            <Form.Item
              label="Telegram"
              name="telegram"
              rules={[
                { required: true, message: 'Пожалуйста, введите Telegram' },
                { min: 3, message: 'Telegram должен содержать минимум 3 символа' },
              ]}
            >
              <Input
                prefix={<MessageOutlined />}
                placeholder="Ваш Telegram username"
              />
            </Form.Item>

            <Form.Item
              label="Город"
              name="city"
              rules={[
                { required: true, message: 'Пожалуйста, введите город' },
                { min: 2, message: 'Город должен содержать минимум 2 символа' },
              ]}
            >
              <Input
                prefix={<HomeOutlined />}
                placeholder="Город проживания"
              />
            </Form.Item>

            <Form.Item
              label="Из какой вы церкви?"
              name="church"
              rules={[{ required: true, message: 'Пожалуйста, введите название церкви' }]}
            >
              <Input placeholder="Название вашей церкви" />
            </Form.Item>

            <Form.Item
              label="Ваша роль"
              name="role"
              rules={[{ required: true, message: 'Пожалуйста, выберите роль' }]}
            >
              <Radio.Group>
                <Space direction="vertical">
                  <Radio value="подросток">Я - подросток</Radio>
                  <Radio value="служитель">Я - подростковый служитель</Radio>
                </Space>
              </Radio.Group>
            </Form.Item>

            <Form.Item name="needAccommodation" valuePropName="checked">
              <Checkbox>Нуждаюсь в расселении</Checkbox>
            </Form.Item>

            <Form.Item name="wasBefore" valuePropName="checked">
              <Checkbox>Уже был(а) на нашей конференции ранее</Checkbox>
            </Form.Item>
          </div>

          {isUnder18 && (
            <>
              <div className="under-18-section">
                <Title level={4}>Информация для несовершеннолетних участников</Title>
                
                <Form.Item
                  label="ФИО одного из родителей"
                  name="parentFullName"
                  rules={isUnder18 ? [{ required: true, message: 'Пожалуйста, введите ФИО родителя' }] : []}
                >
                  <Input placeholder="ФИО родителя" />
                </Form.Item>

                <Form.Item
                  label="Телефон одного из родителей"
                  name="parentPhone"
                  rules={isUnder18 ? [
                    { required: true, message: 'Пожалуйста, введите телефон родителя' },
                    { pattern: /^\+7\d{10}$/, message: 'Формат: +7XXXXXXXXXX' },
                  ] : []}
                >
                  <Input placeholder="Номер телефона" />
                </Form.Item>
              </div>
            </>
          )}

          {/* Согласия */}
          <div className="form-section consent-section">
            <Paragraph style={{ marginBottom: 12, fontWeight: 500 }}>
              Добровольное пожертвование, в размере 500 рублей производится сразу после заполнения всех данных регистрации
            </Paragraph>
            <Paragraph style={{ marginBottom: 18, fontWeight: 500 }}>
              Если тебе меньше 14 лет, твои родители должны подписать согласие и доверенность на участие в христианской конференции.
            </Paragraph>
            
            {isUnder14 && (
              <Form.Item
                name="consentUnder14"
                valuePropName="checked"
                rules={[{ required: isUnder14, message: 'Необходимо согласие для участников младше 14 лет' }]}
              >
                <Checkbox>
                  Я ознакомился с тем, что если мне меньше 14 лет, мои родители должны подписать согласие 
                  и доверенность на моё участие в христианской конференции
                </Checkbox>
              </Form.Item>
            )}

            <Form.Item
              name="consentDonation"
              valuePropName="checked"
              rules={[{ required: true, message: 'Необходимо согласие на пожертвование' }]}
            >
              <Checkbox>
                Я ознакомился с информацией про добровольное пожертвование
              </Checkbox>
            </Form.Item>

            <Form.Item
              name="consentPersonalData"
              valuePropName="checked"
              rules={[{ required: true, message: 'Необходимо согласие на обработку персональных данных' }]}
            >
              <Checkbox>
                Согласие на обработку персональных данных
              </Checkbox>
            </Form.Item>

            {/* <div className="consent-text"> */}
              <Paragraph type="secondary">
                Даю согласие Местной религиозной организации христиан веры евангельской(пятидесятников)Церковь «Слово Жизни»г. Саратова,зарегистрированной по адресу: 410004, г. Саратов, ул. им. Н.Г. Чернышевского,д. 88 (далее — Оператор)и лицам действующим по его поручению на обработку персональных данных моих на указанных ниже условиях.С целью обработки персональных данных является:регистрация и участие в подростковой конференции ТИНС,презентация новостей и церковных мероприятий,проведение совместных служений,подготовка документов по уставным направлением деятельности церкви,создание фото и видео сюжетов о жизни церкви,проведение спортивных,развлекательных и иных мероприятий,направление корреспонденции.Согласие дается в отношении следующих персональных данных:фамилия,имя,дата рождения, город проживания,контактные телефоны,название церкви и служения,в котором принимаю участие.Согласие дается на следующие действия с персональными данными:сбор,систематизация,накопление,хранение,уточнение(обновление, изменение),извлечение,использование, передача(предоставление,доступ),обезличивание,блокирование,удаление,уничтожение персональных данных как с использованием средств автоматизации,так и без таковых.Данное согласие действует с момента его подписания до момента получения Оператором письменного заявления об отзыве настоящего согласия на обработку персональных данных. Согласие может быть отозвано мной путем составления заявления в письменной форме и подачи Оператору.
              </Paragraph>
            {/* </div> */}
          </div>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              loading={loading}
              block
              style={{ height: '50px', fontSize: '18px', fontWeight: 600 }}
            >
              {loading ? 'Регистрация...' : 'Зарегистрироваться'}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default RegistrationPage;