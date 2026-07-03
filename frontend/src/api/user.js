import request from './request';

export const register = (form) => request.post('/user/register', form);
export const login = (form) => request.post('/user/login', form);
export const getMe = () => request.get('/user/me');
export const updateMe = (form) => request.put('/user/me', form);
export const listUsers = () => request.get('/user/list');

export const listGuests = () => request.get('/guest/list');
export const addGuest = (guest) => request.post('/guest', guest);
export const updateGuest = (guest) => request.put('/guest', guest);
export const deleteGuest = (guestId) => request.delete(`/guest/${guestId}`);
