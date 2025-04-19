# Hotel Management System

A Spring Boot application for managing hotel information.

## Deployment to Railway

### Prerequisites
- Railway account (https://railway.app/)
- Git installed on your local machine
- Railway CLI (optional, but recommended)

### Deployment Steps

1. **Install Railway CLI** (Optional)
   ```
   npm i -g @railway/cli
   ```

2. **Login to Railway**
   ```
   railway login
   ```

3. **Build the project**
   ```
   ./gradlew clean build
   ```

4. **Initialize a Railway Project**
   ```
   railway init
   ```

5. **Link to Existing Project** (if you already have one on Railway)
   ```
   railway link
   ```

6. **Deploy to Railway**
   ```
   railway up
   ```

Alternatively, you can deploy directly from GitHub by connecting your GitHub repository to Railway:

1. Go to [Railway Dashboard](https://railway.app/dashboard)
2. Click "New Project"
3. Choose "Deploy from GitHub repo"
4. Select your repository
5. Railway will automatically detect the Spring Boot application and deploy it

## Environment Variables

The following environment variables should be set in Railway:

- `PORT`: The port on which the application will run (Railway sets this automatically)
- `JWT_SECRET`: Secret key used for JWT token generation (optional, defaults to value in application.properties)

## Database Configuration

This application is configured to use the Railway MySQL database. The connection details are automatically configured when you add a MySQL database to your Railway project.

## Endpoints

- `/api/auth/register`: Register a new user
- `/api/auth/login`: Login to get JWT token
- `/api/hotels`: Hotel management endpoints 