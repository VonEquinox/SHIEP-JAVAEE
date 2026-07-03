import request from './request';

export const createOrder = (dto) => request.post('/order', dto);
export const cancelOrder = (orderId) => request.put(`/order/${orderId}/cancel`);
export const myOrders = (status) => request.get('/order/mine', { params: { status } });
export const orderDetail = (orderId) => request.get(`/order/${orderId}`);
export const adminOrders = (status) => request.get('/order/list', { params: { status } });
export const updateOrderStatus = (orderId, status) =>
  request.put(`/order/${orderId}/status`, null, { params: { status } });

export const addReview = (review) => request.post('/review', review);
export const hotelReviews = (hotelId) => request.get(`/review/hotel/${hotelId}`);

export const statsBooking = (params) => request.get('/stats/booking', { params });
export const statsGuest = (params) => request.get('/stats/guest', { params });
export const statsOccupancy = (params) => request.get('/stats/occupancy', { params });
