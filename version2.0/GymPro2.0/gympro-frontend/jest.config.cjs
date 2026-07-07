/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/src/__tests__/setupTests.js'],
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
    '\\.(jpg|jpeg|png|gif|svg|webp)$': '<rootDir>/src/__tests__/__mocks__/fileMock.js',
  },
  transform: {
    '^.+\\.[jt]sx?$': 'babel-jest',
  },
  testMatch: ['**/__tests__/**/*.test.[jt]s?(x)'],
  collectCoverageFrom: [
    'src/**/*.{js,jsx}',
    '!src/main.jsx',
    '!src/**/*.d.ts',
  ],
};
