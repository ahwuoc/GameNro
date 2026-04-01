# ✅ Refactoring Checklist

Use this checklist for each module refactoring task.

## 📋 Pre-Refactoring

- [ ] Read and understand current code
- [ ] Document current behavior
- [ ] Identify all callers (use `rg "module_name::"`)
- [ ] Run existing tests and ensure they pass
- [ ] Create baseline performance benchmark
- [ ] Create feature branch: `refactor/module-name`

## 🔨 During Refactoring

### Planning
- [ ] Identify logical groupings of functions
- [ ] Design target module structure
- [ ] Plan migration strategy
- [ ] Identify potential breaking changes

### Implementation
- [ ] Create target directory structure
- [ ] Create `mod.rs` with exports
- [ ] Move functions one group at a time
- [ ] Update imports after each group
- [ ] Run tests after each group
- [ ] Commit after each successful group

### Code Quality
- [ ] Remove code duplication
- [ ] Apply early returns to reduce nesting
- [ ] Extract complex logic to helper functions
- [ ] Add inline documentation
- [ ] Follow Rust naming conventions

## 🧪 Testing

- [ ] Write unit tests for new modules
- [ ] Update existing tests
- [ ] Run full test suite: `cargo test`
- [ ] Run clippy: `cargo clippy`
- [ ] Run fmt: `cargo fmt`
- [ ] Performance benchmark: `cargo bench`
- [ ] Manual testing of affected features

## 📚 Documentation

- [ ] Update module-level documentation
- [ ] Add examples if needed
- [ ] Update architecture docs
- [ ] Update CHANGELOG.md
- [ ] Add migration notes if breaking changes

## 🔍 Code Review

- [ ] Self-review all changes
- [ ] Check for unused imports
- [ ] Check for dead code
- [ ] Verify error handling
- [ ] Check for potential panics

## ✅ Post-Refactoring

- [ ] All tests passing
- [ ] No clippy warnings
- [ ] Code formatted
- [ ] Documentation updated
- [ ] Performance acceptable (< 5% regression)
- [ ] Create pull request
- [ ] Get code review
- [ ] Address review comments
- [ ] Merge to main branch

## 📊 Metrics Verification

- [ ] File size < 400 lines
- [ ] No functions > 50 lines
- [ ] Cyclomatic complexity acceptable
- [ ] Test coverage > 60%
- [ ] No code duplication > 3 times

## 🎉 Completion

- [ ] Update refactoring roadmap
- [ ] Close related issues
- [ ] Celebrate! 🎊

---

## 📝 Notes Template

```markdown
## Refactoring: [MODULE_NAME]

**Date:** YYYY-MM-DD
**Author:** Your Name

### Changes Made
- Moved X functions to Y module
- Created Z new modules
- Reduced file size from A to B lines

### Breaking Changes
- None / List breaking changes

### Migration Guide
- How to update code that uses this module

### Performance Impact
- Benchmark results before/after

### Lessons Learned
- What went well
- What could be improved
```
