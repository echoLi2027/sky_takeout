# Sky Takeout - Restaurant Management System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.5.9-blue.svg)](https://mybatis.org/mybatis-3/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📋 Overview

Sky Takeout is a comprehensive restaurant management system designed for food service businesses. The system consists of two main components: an administrative backend for restaurant staff and a WeChat mini-program for customers. This full-stack solution streamlines restaurant operations from menu management to order processing and delivery.

### 🎯 Key Features

- **Dual-Platform Architecture**: Separate interfaces for restaurant management and customer ordering
- **Real-Time Order Management**: Live order tracking and status updates
- **Comprehensive Analytics**: Business intelligence dashboard with revenue and order statistics
- **WeChat Integration**: Native mini-program support for customer convenience
- **Scalable Design**: Microservices architecture supporting high-concurrency scenarios

## 🚀 Features

### Admin Portal (Restaurant Staff)

- **Employee Management**: Complete CRUD operations for staff accounts with role-based access control
- **Category Management**: Organize dishes and combo meals into logical categories
- **Dish Management**: Add, edit, and manage individual dishes with pricing and availability
- **Combo Meal Management**: Create and manage set meals with bundled dishes
- **Order Management**: Real-time order processing, status tracking, and delivery coordination
- **Statistical Reports**: Revenue analysis, user statistics, and order trends visualization
- **Real-time Notifications**: WebSocket-based order alerts and催单 (rush order) notifications

### Customer Mini-Program

- **WeChat Authentication**: Seamless login via WeChat OAuth
- **Menu Browsing**: Interactive menu with categories, search, and filtering
- **Shopping Cart**: Add, remove, and modify items before checkout
- **Address Management**: Save and manage multiple delivery addresses
- **Order Placement**: Streamlined checkout process with multiple payment options
- **Order History**: Track past orders and reorder favorites
- **Real-time Updates**: Live order status tracking

## 🛠 Tech Stack

### Backend

- **Framework**: Spring Boot 2.7.3
- **Web**: Spring MVC
- **Persistence**: MyBatis 3.5.9, PageHelper
- **Database**: MySQL 8.0
- **Cache**: Redis 7.0, Spring Cache
- **API Documentation**: Swagger/Knife4j
- **Authentication**: JWT (JSON Web Tokens)
- **Task Scheduling**: Spring Task
- **File Storage**: Aliyun OSS
- **Communication**: WebSocket
- **Utilities**: Apache POI (Excel), HttpClient

### Frontend

- **Admin Portal**: Vue.js 3.0, Element UI, Apache ECharts
- **Customer App**: WeChat Mini-Program
- **Web Server**: Nginx (reverse proxy & load balancing)

### DevOps & Tools

- **Version Control**: Git
- **Build Tool**: Maven
- **Testing**: JUnit, Postman
- **Containerization**: Docker (optional)

## 📁 Project Structure

```
sky-take-out/
├── sky-common/          # Common utilities and constants
│   ├── constant/        # System constants
│   ├── context/         # Context holders
│   ├── enumeration/     # Enumerations
│   ├── exception/       # Custom exceptions
│   ├── properties/      # Configuration properties
│   ├── result/          # Response wrappers
│   └── utils/          # Utility classes
│
├── sky-pojo/           # Data models
│   ├── dto/            # Data Transfer Objects
│   ├── entity/         # Database entities
│   └── vo/             # View Objects
│
└── sky-server/         # Main application
    ├── config/         # Configuration classes
    ├── controller/     # REST controllers
    ├── interceptor/    # HTTP interceptors
    ├── mapper/         # MyBatis mappers
    ├── service/        # Business logic
    └── SkyApplication.java # Main class
```

## 💾 Database Schema

The system uses 11 core tables:

| Table             | Description                               |
| ----------------- | ----------------------------------------- |
| `employee`      | Restaurant staff accounts and permissions |
| `category`      | Dish and combo meal categories            |
| `dish`          | Individual dish items                     |
| `dish_flavor`   | Dish customization options                |
| `setmeal`       | Combo meal packages                       |
| `setmeal_dish`  | Combo meal dish relationships             |
| `user`          | Customer accounts                         |
| `address_book`  | Delivery addresses                        |
| `shopping_cart` | Shopping cart items                       |
| `orders`        | Order records                             |
| `order_detail`  | Order line items                          |

## 🚀 Getting Started

### Prerequisites

- JDK 11 or higher
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+
- Node.js 14+ (for frontend development)
- Git

### Installation

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/sky-takeout.git
cd sky-takeout
```

2. **Database Setup**

```bash
# Create database and import schema
mysql -u root -p
source sky.sql
```

3. **Configure application properties**

```yaml
# Edit application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sky_take_out
    username: your_username
    password: your_password
  
  redis:
    host: localhost
    port: 6379
```

4. **Build the project**

```bash
mvn clean install
```

5. **Run the application**

```bash
java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

6. **Start Nginx for frontend**

```bash
# Navigate to nginx directory
cd nginx-1.20.2
# Start nginx
nginx.exe  # Windows
nginx      # Linux/Mac
```

7. **Access the application**

- Admin Portal: http://localhost/
- API Documentation: http://localhost:8080/doc.html
- Default credentials: admin/123456

## 📡 API Documentation

The project uses Swagger/Knife4j for API documentation. Once the application is running, access the interactive API documentation at:

```
http://localhost:8080/doc.html
```

### Key API Endpoints

```
POST   /admin/employee/login     - Employee login
GET    /admin/category/list      - List categories
POST   /admin/dish               - Add new dish
PUT    /admin/dish               - Update dish
GET    /admin/order/list         - List orders
PUT    /admin/order/confirm      - Confirm order
```

## 🔧 Configuration

### Nginx Reverse Proxy

```nginx
server {
    listen 80;
    server_name localhost;
  
    location /api/ {
        proxy_pass http://localhost:8080/admin/;
    }
  
    location / {
        root html/sky;
        index index.html;
    }
}
```

### JWT Configuration

```yaml
sky:
  jwt:
    admin-secret-key: your-secret-key
    admin-ttl: 7200000
    user-secret-key: your-user-secret
    user-ttl: 7200000
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# API testing with Postman
# Import the provided Postman collection from /docs/api
```

## 📈 Performance Optimization

- **Caching Strategy**: Redis caching for frequently accessed data (menu items, categories)
- **Database Optimization**: Indexed queries, connection pooling
- **Load Balancing**: Nginx upstream configuration for horizontal scaling
- **Async Processing**: Spring Task for background jobs

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 Development Workflow

1. **Requirements Analysis**: Review product requirements and prototypes
2. **API Design**: Define endpoints using API-first approach
3. **Implementation**: Develop features following TDD principles
4. **Testing**: Unit tests, integration tests, and manual testing
5. **Documentation**: Update API docs and user guides
6. **Code Review**: Peer review before merging
7. **Deployment**: CI/CD pipeline for automated deployment

## 🔒 Security Features

- JWT-based authentication
- Password encryption (MD5)
- Role-based access control (RBAC)
- SQL injection prevention via MyBatis
- XSS protection
- CORS configuration

## 📊 Monitoring & Logging

- Structured logging with SLF4J/Logback
- Request/Response interceptors for audit trails
- Performance metrics collection
- Error tracking and alerting

## 🌍 Internationalization

Currently supports:

- Chinese (Simplified)
- English (planned)

## 📱 Mobile Support

- Responsive admin dashboard
- Native WeChat Mini-Program
- Mobile-optimized user experience

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

For questions or support, please contact:

- Email: echozzy1126@gmail.com

## 🗺 Roadmap

- [ ] Multi-language support
- [ ] Mobile app (iOS/Android)
- [ ] AI-powered recommendation system
- [ ] Integration with third-party delivery platforms
- [ ] Advanced analytics dashboard
- [ ] Loyalty program features
- [ ] Inventory management system
- [ ] Kitchen display system integration

---

⭐️ If you find this project useful, please consider giving it a star!
