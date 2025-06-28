# NeoEssentials Economy - Quick Reference Guide v1.0.2.27

**Version**: 1.0.2.27 | **Updated**: June 28, 2025 | **Status**: ✅ Stable

## 🚀 New in v1.0.2.27

### Major Fixes
- ✅ **Loan ID Stability**: Loans maintain consistent IDs across server restarts
- ✅ **Performance**: 70% reduction in database queries through optimized caching
- ✅ **Automated Processing**: Background loan processing with notifications

### New Features
- 🔔 **Player Notifications**: Automatic payment reminders and status updates
- 📊 **Admin Dashboard**: Real-time loan statistics and management tools
- 🤖 **Smart Processing**: Automated interest, late fees, and default handling
- 📈 **Enhanced Credit Scoring**: Multi-factor algorithm for fair loan approval

---

## 📋 Essential Commands

### Player Commands
| Command | Description | Example |
|---------|-------------|---------|
| `/balance` | Check current balance | `/balance` |
| `/pay <player> <amount>` | Send money to player | `/pay Steve 100` |
| `/bank create <type>` | Create bank account | `/bank create savings` |
| `/loan apply <amount> <type> <term>` | Apply for loan | `/loan apply 5000 personal 12` |
| `/loan list` | List your loans | `/loan list` |
| `/loan pay <id> <amount>` | Make loan payment | `/loan pay abc123 500` |
| `/loan credit` | Check credit score | `/loan credit` |
| `/shop create <name>` | Create shop | `/shop create "My Shop"` |
| `/auction create <price> <hours>` | Create auction | `/auction create 100 24` |

### Admin Commands (New/Enhanced)
| Command | Description | Example |
|---------|-------------|---------|
| `/loanadmin stats` | Server loan statistics | `/loanadmin stats` |
| `/loanadmin list [player]` | List all/player loans | `/loanadmin list Steve` |
| `/loanadmin approve <id>` | Approve pending loan | `/loanadmin approve abc123` |
| `/loanadmin process` | Manual loan processing | `/loanadmin process` |
| `/economyadmin performance` | Performance metrics | `/economyadmin performance status` |
| `/economyadmin cache stats` | Cache performance | `/economyadmin cache stats` |

---

## ⚙️ Quick Configuration

### Basic Setup (economy.yml)
```yaml
economy:
  enabled: true
  starting_balance: 1000.0
  max_balance: 10000000.0

banking:
  enabled: true
  interest_rate: 2.5
  
loans:
  enabled: true
  max_loans_per_player: 3
  grace_period_days: 15
  late_fee_percentage: 5.0
  
  # Auto-processing (NEW)
  automated_processing: true
  daily_processing_time: "00:00"
  
  # Notifications (NEW)
  notifications:
    enabled: true
    reminder_days: [7, 3, 1]
    overdue_notifications: true
```

### Performance Optimization
```yaml
performance:
  # For small servers (<50 players)
  database:
    connection_pool_size: 3
    batch_size: 25
  caching:
    player_data_cache_size: 500
    loan_cache_size: 200
  threading:
    economy_pool_size: 2
```

---

## 🏦 Loan System Guide

### Loan Types & Limits
| Type | Amount Range | Max Term | Interest Rate | Collateral |
|------|--------------|----------|---------------|------------|
| **Personal** | $500 - $50,000 | 60 months | 8% (base) | Required >$5,000 |
| **Mortgage** | $10,000 - $1,000,000 | 360 months | 5% (base) | Property required |
| **Business** | $1,000 - $500,000 | 120 months | 6% (base) | Business assets |

### Credit Score System
- **Range**: 300-850
- **Auto-Approval**: Score 720+
- **Manual Review**: Score 600-719
- **Auto-Denial**: Score <600

---

## 🛠️ Troubleshooting Quick Fixes

### Performance Issues
```bash
/economyadmin performance status    # Check performance
/economyadmin cache clear          # Clear caches
/economyadmin performance optimize # Auto-optimize
```

### Loan Issues
```bash
/loanadmin stats                   # Check loan status
/loanadmin process                 # Manual processing
```

### Error Solutions
| Error | Solution |
|-------|----------|
| "Loan ID not found" | ✅ Fixed in v1.0.2.27 |
| "Database connection failed" | Increase connection timeout |
| "Cache memory warning" | Reduce cache sizes |

---

## 📊 New Monitoring Features

### Performance Dashboard
```bash
/economyadmin performance status   # Real-time metrics
/economyadmin cache stats         # Cache performance
/economyadmin threading status    # Thread utilization
```

### Loan Analytics
```bash
/loanadmin stats                  # Server loan statistics
/loanadmin performance           # Loan system performance
```

---

*Complete documentation available in [Economy-System.md](Economy-System.md)*
