import axios from 'axios';

export const API_URL = process.env.REACT_APP_API_URL;

axios.defaults.baseURL = API_URL;
axios.defaults.timeout = 15000;

export default axios;