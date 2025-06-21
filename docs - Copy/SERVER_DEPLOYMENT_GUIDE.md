# NeoEssentials Server Deployment Guide

## Overview

NeoEssentials is now optimized for server-side deployment with minimal client requirements. This guide explains how to deploy NeoEssentials in various scenarios and what to expect.

## Deployment Options

### Option 1: Server-Only (Advanced)

**Setup:**
- Install NeoEssentials on your server only
- Clients connect without any mods

**Benefits:**
- Simplified client experience
- No mod installation required for players
- Full admin functionality on the server

**Considerations:**
- Some clients might experience connection issues
- Advanced command features may not work for all clients

### Option 2: Server + Client (Recommended)

**Setup:**
- Install NeoEssentials on your server
- Ask players to install the same version on their clients

**Benefits:**
- Guaranteed compatibility
- All features work properly
- No connection issues

**Considerations:**
- Requires players to install a mod
- Slightly more complex setup

## How It Works

NeoEssentials now uses advanced techniques to maximize server-side functionality with minimal client impact:

1. **Multi-Stage Registration**: Command features register in multiple ways to maximize compatibility
2. **Enhanced Error Handling**: Robust fallback mechanisms if things don't match perfectly
3. **Optimized Network Code**: Minimizes what needs to be synchronized between server and client

## For Server Administrators

If players experience connection issues with the server-only deployment:

1. First, try updating to the latest version of NeoEssentials, which may have compatibility improvements
2. If issues persist, ask players to install the NeoEssentials mod on their clients
3. Make sure both server and clients are using the same version of the mod

## Technical Background

Minecraft requires certain registry information to be synchronized between server and client. For custom command types (which NeoEssentials uses), this synchronization typically requires both sides to have the mod installed.

NeoEssentials has implemented advanced techniques to try making server-only deployment work, but due to Minecraft's architecture, this approach may not be 100% reliable for all clients.

## Testing Your Setup

We recommend testing both deployment options in a controlled environment before deciding which approach works best for your server:

1. Set up a test server with NeoEssentials
2. Try connecting with:
   - A vanilla client (no mods)
   - A client with NeoEssentials installed

This will help you determine which approach best suits your community's needs.
