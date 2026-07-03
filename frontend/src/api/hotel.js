import request from './request';

export const searchHotels = (params) => request.get('/hotel/search', { params });
export const getHotelRooms = (hotelId) => request.get(`/hotel/${hotelId}/rooms`);
export const getCities = () => request.get('/hotel/cities');
export const saveHotel = (hotel) => request.post('/hotel', hotel);
export const saveRoomType = (room) => request.post('/room', room);
export const deleteRoomType = (roomTypeId) => request.delete(`/room/${roomTypeId}`);
