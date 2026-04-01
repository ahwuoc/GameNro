---
name: Refactoring Task
about: Track refactoring of a specific module
title: 'Refactor: [MODULE_NAME]'
labels: refactoring, technical-debt
assignees: ''
---

## 📋 Module Information

**File:** `src/path/to/file.rs`  
**Current Size:** XXX lines  
**Target Size:** < 400 lines  
**Priority:** P0 / P1 / P2 / P3  
**Estimated Effort:** X days

## 🎯 Refactoring Goals

- [ ] Reduce file size to < 400 lines
- [ ] Separate concerns into logical modules
- [ ] Improve testability
- [ ] Add/update tests
- [ ] Update documentation

## 📁 Target Structure

```
target/directory/
├── mod.rs
├── submodule1.rs
├── submodule2.rs
└── submodule3.rs
```

## ✅ Checklist

### Analysis
- [ ] Identify all functions in module
- [ ] Group functions by domain/responsibility
- [ ] Identify dependencies
- [ ] Document current behavior

### Implementation
- [ ] Create target directory structure
- [ ] Create submodule files
- [ ] Move functions to submodules
- [ ] Update imports in mod.rs
- [ ] Update all callers

### Testing
- [ ] Write/update unit tests
- [ ] Run integration tests
- [ ] Performance benchmark
- [ ] Manual testing

### Documentation
- [ ] Update module documentation
- [ ] Update architecture docs
- [ ] Add migration notes

### Cleanup
- [ ] Remove old code
- [ ] Remove unused imports
- [ ] Run clippy
- [ ] Run fmt

## 📊 Metrics

**Before:**
- Lines of code: XXX
- Functions: XX
- Complexity: High/Medium/Low

**After:**
- Lines of code: XXX (target: < 400)
- Number of modules: X
- Complexity: High/Medium/Low

## 🔗 Related Issues

- Related to #XXX
- Blocks #XXX
- Depends on #XXX

## 📝 Notes

Add any additional notes, concerns, or decisions here.
