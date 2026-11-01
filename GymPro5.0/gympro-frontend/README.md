# GymPro React Frontend

React frontend for the GymPro Gym Management System.  
Integrates with the **auth-service** via API Gateway for JWT-based login and role-based routing.

---

## Setup & Run

```bash
# 1. Install dependencies
npm install

# 2. Start GymPro backend first (Eureka + Gateway + auth-service)
#    Make sure API Gateway runs on port 8080

# 3. Start React dev server
npm run dev
# → Opens at http://localhost:3000
```

---

## Project Structure

```
src/
├── api/
│   ├── axiosInstance.js     ← Axios with JWT interceptor (auto-attaches Bearer token)
│   └── authApi.js           ← login() and register() API calls
├── auth/
│   ├── Login.jsx            ← Login form → POST /api/auth/login
│   └── Register.jsx         ← Register form → POST /api/auth/register
├── components/
│   ├── Navbar.jsx           ← Top nav with role badge + logout
│   └── ProtectedRoute.jsx   ← Guards routes by token + role
├── context/
│   └── AuthContext.jsx      ← Global auth state (token, role, login, logout)
├── pages/
│   ├── AdminDashboard.jsx   ← ADMIN only
│   ├── TrainerDashboard.jsx ← TRAINER + ADMIN
│   ├── MemberDashboard.jsx  ← MEMBER + ADMIN
│   └── Forbidden.jsx        ← 403 page
├── App.jsx                  ← Route definitions
└── main.jsx                 ← Entry point
```

---

## Role-Based Access

| Route      | Allowed Roles        |
|------------|----------------------|
| `/admin`   | ADMIN                |
| `/trainer` | TRAINER, ADMIN       |
| `/member`  | MEMBER, ADMIN        |
| `/login`   | Public               |
| `/register`| Public               |

---

## Auth Flow

1. User submits login form
2. React calls `POST /api/auth/login` via API Gateway → auth-service
3. auth-service returns `{ token, role }`
4. Token + role stored in `localStorage`
5. Axios interceptor attaches `Authorization: Bearer <token>` to every request
6. `ProtectedRoute` checks token + role before rendering pages
7. On 401 response → auto logout + redirect to `/login`

---

## Backend Endpoints (auth-service via Gateway)

| Method | Endpoint             | Description               |
|--------|----------------------|---------------------------|
| POST   | /api/auth/register   | Register new user         |
| POST   | /api/auth/login      | Login → returns JWT token |
| GET    | /api/auth/validate   | Validate token (Gateway)  |
