package staffschedule.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.optaplanner.core.api.solver.change.ProblemChangeDirector;

/**
 * Test double for {@link ProblemChangeDirector} that applies each change's
 * consumer directly to the given object (as the real director does to the
 * working solution) and records what was changed. Lets a
 * {@link org.optaplanner.core.api.solver.change.ProblemChange#doChange} be unit
 * tested against an in-memory {@link com.midco.rota.model.Rota} with no solver.
 */
public class RecordingProblemChangeDirector implements ProblemChangeDirector {

	public final List<String> changedVariables = new ArrayList<>();
	public int problemPropertyChanges = 0;
	public int addedEntities = 0;
	public int removedEntities = 0;
	public int addedFacts = 0;
	public int removedFacts = 0;
	public int shadowVariableUpdates = 0;

	@Override
	public <Entity> void addEntity(Entity entity, Consumer<Entity> entityConsumer) {
		entityConsumer.accept(entity);
		addedEntities++;
	}

	@Override
	public <Entity> void removeEntity(Entity entity, Consumer<Entity> entityConsumer) {
		entityConsumer.accept(entity);
		removedEntities++;
	}

	@Override
	public <Entity> void changeVariable(Entity entity, String variableName, Consumer<Entity> entityConsumer) {
		entityConsumer.accept(entity);
		changedVariables.add(variableName);
	}

	@Override
	public <ProblemFact> void addProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer) {
		problemFactConsumer.accept(problemFact);
		addedFacts++;
	}

	@Override
	public <ProblemFact> void removeProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer) {
		problemFactConsumer.accept(problemFact);
		removedFacts++;
	}

	@Override
	public <EntityOrProblemFact> void changeProblemProperty(EntityOrProblemFact problemFactOrEntity,
			Consumer<EntityOrProblemFact> consumer) {
		consumer.accept(problemFactOrEntity);
		problemPropertyChanges++;
	}

	@Override
	public <EntityOrProblemFact> EntityOrProblemFact lookUpWorkingObjectOrFail(EntityOrProblemFact externalObject) {
		return externalObject;
	}

	@Override
	public <EntityOrProblemFact> Optional<EntityOrProblemFact> lookUpWorkingObject(EntityOrProblemFact externalObject) {
		return Optional.ofNullable(externalObject);
	}

	@Override
	public void updateShadowVariables() {
		shadowVariableUpdates++;
	}
}
