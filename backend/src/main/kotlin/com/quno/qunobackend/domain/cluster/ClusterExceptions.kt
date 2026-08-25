package com.quno.qunobackend.domain.cluster

class CannotClusterWithSelfException(questionId: Long) :
    RuntimeException("Cannot mark a question as the same problem as itself: $questionId")

class ClustersAlreadyDistinctException(clusterIdA: Long, clusterIdB: Long) :
    RuntimeException(
        "Questions already belong to different clusters ($clusterIdA, $clusterIdB) — merging clusters is not supported yet",
    )

class ClusterNotFoundException(id: Long) : RuntimeException("Cluster not found: $id")

class QuestionNotInAnyClusterException(questionId: Long) :
    RuntimeException("Question does not belong to any cluster: $questionId")

class AnswerNotInClusterException(answerId: Long, clusterId: Long) :
    RuntimeException("Answer $answerId does not belong to a question in cluster $clusterId")

class AnswerNotAcceptedException(answerId: Long) :
    RuntimeException("Only an accepted answer can become a Super Answer: $answerId")
