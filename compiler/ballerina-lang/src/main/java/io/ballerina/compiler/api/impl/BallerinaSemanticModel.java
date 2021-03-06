/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.compiler.api.impl;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.model.symbols.SymbolKind;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ballerinalang.model.symbols.SymbolOrigin.COMPILED_SOURCE;
import static org.ballerinalang.model.symbols.SymbolOrigin.SOURCE;

/**
 * Semantic model representation of a given syntax tree.
 *
 * @since 2.0.0
 */
public class BallerinaSemanticModel implements SemanticModel {

    private final BLangPackage bLangPackage;
    private final CompilerContext compilerContext;
    private final EnvironmentResolver envResolver;

    public BallerinaSemanticModel(BLangPackage bLangPackage, CompilerContext context) {
        this.compilerContext = context;
        this.bLangPackage = bLangPackage;

        SymbolTable symbolTable = SymbolTable.getInstance(context);
        SymbolEnv pkgEnv = symbolTable.pkgEnvMap.get(bLangPackage.symbol);
        this.envResolver = new EnvironmentResolver(pkgEnv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Symbol> visibleSymbols(String fileName, LinePosition linePosition) {
        List<Symbol> compiledSymbols = new ArrayList<>();
        SymbolResolver symbolResolver = SymbolResolver.getInstance(this.compilerContext);
        BLangCompilationUnit compilationUnit = getCompilationUnit(fileName);
        Map<Name, List<Scope.ScopeEntry>> scopeSymbols =
                symbolResolver.getAllVisibleInScopeSymbols(this.envResolver.lookUp(compilationUnit, linePosition));

        Location cursorPos = new BLangDiagnosticLocation(compilationUnit.name,
                                                    linePosition.line(), linePosition.line(),
                                                    linePosition.offset(), linePosition.offset());

        for (Map.Entry<Name, List<Scope.ScopeEntry>> entry : scopeSymbols.entrySet()) {
            Name name = entry.getKey();
            List<Scope.ScopeEntry> scopeEntries = entry.getValue();

            for (Scope.ScopeEntry scopeEntry : scopeEntries) {
                BSymbol symbol = scopeEntry.symbol;

                if (hasCursorPosPassedSymbolPos(symbol, cursorPos) || isImportedSymbol(symbol)) {
                    compiledSymbols.add(SymbolFactory.getBCompiledSymbol(symbol, name.getValue()));
                }
            }
        }

        return compiledSymbols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Symbol> symbol(String fileName, LinePosition position) {
        BLangCompilationUnit compilationUnit = getCompilationUnit(fileName);
        SymbolFinder symbolFinder = new SymbolFinder();
        BSymbol symbolAtCursor = symbolFinder.lookup(compilationUnit, position);

        if (symbolAtCursor == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(SymbolFactory.getBCompiledSymbol(symbolAtCursor, symbolAtCursor.name.value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Symbol> moduleLevelSymbols() {
        List<Symbol> compiledSymbols = new ArrayList<>();

        for (Map.Entry<Name, Scope.ScopeEntry> e : bLangPackage.symbol.scope.entries.entrySet()) {
            Name key = e.getKey();
            Scope.ScopeEntry value = e.getValue();

            if (value.symbol.origin == SOURCE) {
                compiledSymbols.add(SymbolFactory.getBCompiledSymbol(value.symbol, key.value));
            }
        }

        return compiledSymbols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TypeSymbol> getType(String fileName, LineRange range) {
        BLangCompilationUnit compilationUnit = getCompilationUnit(fileName);
        NodeFinder nodeFinder = new NodeFinder();
        BLangNode node = nodeFinder.lookup(compilationUnit, range);

        if (node == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(TypesFactory.getTypeDescriptor(node.type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Diagnostic> diagnostics(LineRange range) {
        List<Diagnostic> allDiagnostics = this.bLangPackage.getDiagnostics();
        List<Diagnostic> filteredDiagnostics = new ArrayList<>();

        for (Diagnostic diagnostic : allDiagnostics) {
            LineRange lineRange = diagnostic.location().lineRange();

            if (lineRange.filePath().equals(range.filePath()) && withinRange(lineRange, range)) {
                filteredDiagnostics.add(diagnostic);
            }
        }

        return filteredDiagnostics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Diagnostic> diagnostics() {
        return this.bLangPackage.getDiagnostics();
    }

    // Private helper methods for the public APIs above.

    private boolean hasCursorPosPassedSymbolPos(BSymbol symbol, Location cursorPos) {
        if (symbol.origin != SOURCE) {
            return false;
        }

        if (symbol.owner.getKind() == SymbolKind.PACKAGE || Symbols.isFlagOn(symbol.flags, Flags.WORKER)) {
            return true;
        }

        if (!bLangPackage.packageID.equals(symbol.pkgID)) {
            return false;
        }

        // These checks whether the cursor position has passed the symbol position or not
        LinePosition cursorPosStartLine = cursorPos.lineRange().startLine();
        LinePosition symbolStartLine = symbol.pos.lineRange().startLine();

        if (cursorPosStartLine.line() < symbolStartLine.line()) {
            return false;
        }

        if (cursorPosStartLine.line() > symbolStartLine.line()) {
            return true;
        }

        return cursorPosStartLine.offset() > symbolStartLine.offset();
    }

    private boolean isImportedSymbol(BSymbol symbol) {
        return symbol.origin == COMPILED_SOURCE &&
                (Symbols.isFlagOn(symbol.flags, Flags.PUBLIC) || symbol.getKind() == SymbolKind.PACKAGE);
    }

    private BLangCompilationUnit getCompilationUnit(String srcFile) {
        return bLangPackage.compUnits.stream()
                .filter(unit -> unit.name.equals(srcFile))
                .findFirst()
                .get();
    }

    private boolean withinRange(LineRange range, LineRange specifiedRange) {
        int startLine = range.startLine().line();
        int startOffset = range.startLine().offset();

        int specifiedStartLine = specifiedRange.startLine().line();
        int specifiedEndLine = specifiedRange.endLine().line();
        int specifiedStartOffset = specifiedRange.startLine().offset();
        int specifiedEndOffset = specifiedRange.endLine().offset();

        return startLine >= specifiedStartLine && startLine <= specifiedEndLine &&
                startOffset >= specifiedStartOffset && startOffset <= specifiedEndOffset;
    }
}
