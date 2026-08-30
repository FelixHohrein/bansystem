package net.baublase.bansystem.application;

import net.baublase.bansystem.config.BanTemplateYamlStore;
import net.baublase.bansystem.domain.template.BanTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class TemplateService {

    private final BanTemplateYamlStore store;

    public TemplateService(BanTemplateYamlStore store) {
        this.store = store;
    }

    public List<BanTemplate> list() {
        return store.all();
    }

    public Optional<BanTemplate> find(String id) {
        return store.find(id);
    }

    public void upsert(BanTemplate template) {
        try {
            store.upsert(template);
        } catch (IOException exception) {
            throw new IllegalStateException("Template konnte nicht gespeichert werden", exception);
        }
    }

    public boolean delete(String id) {
        try {
            return store.delete(id);
        } catch (IOException exception) {
            throw new IllegalStateException("Template konnte nicht gelöscht werden", exception);
        }
    }
}
