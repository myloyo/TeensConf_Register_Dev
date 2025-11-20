import React from 'react';
import { ConfigProvider, App as AntdApp } from 'antd';
import ruRU from 'antd/locale/ru_RU';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import RegistrationPage from './pages/RegisterPage';
import PaymentPage from './pages/PaymentPage';
import SuccessPage from './pages/SuccessPage';
import 'antd/dist/reset.css';
import './App.scss';

const App: React.FC = () => {
  return (
    <ConfigProvider locale={ruRU}>
      <AntdApp>
        <Router basename="/">
          <div className="app">
            <Routes>
              <Route path="/" element={<RegistrationPage />} />
              <Route path="/payment" element={<PaymentPage />} />
              <Route path="/success" element={<SuccessPage />} />
              {/* Редирект для всех неизвестных маршрутов */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </Router>
      </AntdApp>
    </ConfigProvider>
  );
};

export default App;