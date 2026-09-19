/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/v1/:path*',
        destination: `${process.env.BACKEND_URL}/api/v1/:path*`,
      },
      {
        source: '/auth/:path*',
        destination: `${process.env.BACKEND_URL}/auth/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
