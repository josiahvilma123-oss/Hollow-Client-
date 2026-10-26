package dev.hollow.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
	private final Map<String, Module> modules = new LinkedHashMap<>();

	public <M extends Module> M register(M module) {
		if (modules.putIfAbsent(module.id(), module) != null) {
			throw new IllegalArgumentException("Duplicate module id " + module.id());
		}
		return module;
	}

	public List<Module> all() {
		return Collections.unmodifiableList(new ArrayList<>(modules.values()));
	}

	public List<Module> byCategory(Category category) {
		return modules.values().stream().filter(m -> m.category() == category).toList();
	}

	public Optional<Module> byId(String id) {
		return Optional.ofNullable(modules.get(id));
	}

	public <M extends Module> M get(Class<M> type) {
		for (Module module : modules.values()) {
			if (type.isInstance(module)) {
				return type.cast(module);
			}
		}
		throw new IllegalStateException("Module not registered: " + type.getSimpleName());
	}
}
