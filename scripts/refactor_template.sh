#!/bin/bash
# Template script for refactoring a module

if [ $# -lt 2 ]; then
    echo "Usage: $0 <module_name> <target_dir>"
    echo "Example: $0 clan_service src/clan/services"
    exit 1
fi

MODULE_NAME=$1
TARGET_DIR=$2

echo "🔧 Refactoring: $MODULE_NAME"
echo "📁 Target directory: $TARGET_DIR"
echo ""

# Create target directory
mkdir -p "$TARGET_DIR"

# Create mod.rs
cat > "$TARGET_DIR/mod.rs" << 'EOF'
// Module exports
// TODO: Add module exports here
EOF

echo "✅ Created $TARGET_DIR/mod.rs"

# Create template files based on common patterns
echo ""
echo "📝 Suggested submodules (create as needed):"
echo "  - $TARGET_DIR/core.rs (core functionality)"
echo "  - $TARGET_DIR/handlers.rs (request handlers)"
echo "  - $TARGET_DIR/validation.rs (validation logic)"
echo "  - $TARGET_DIR/utils.rs (utility functions)"

echo ""
echo "📋 Next steps:"
echo "  1. Analyze $MODULE_NAME and identify logical groups"
echo "  2. Create submodule files in $TARGET_DIR"
echo "  3. Move functions to appropriate submodules"
echo "  4. Update imports in $TARGET_DIR/mod.rs"
echo "  5. Update callers to use new module structure"
echo "  6. Run tests: cargo test"
echo "  7. Remove old code after verification"

echo ""
echo "🔍 Find all usages:"
echo "  rg \"$MODULE_NAME::\" --type rust"
