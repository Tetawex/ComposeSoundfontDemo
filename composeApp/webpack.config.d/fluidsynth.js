// Webpack configuration for FluidSynth library integration
const path = require('path');

// Configure module resolution
config.resolve = config.resolve || {};
config.resolve.alias = config.resolve.alias || {};

// Ensure proper resolution of the FluidSynth library
config.resolve.fallback = config.resolve.fallback || {};
config.resolve.fallback.fs = false;
config.resolve.fallback.path = false;

// Ignore FluidSynth library warnings
config.ignoreWarnings = config.ignoreWarnings || [];
config.ignoreWarnings.push(/Failed to parse source map/);

